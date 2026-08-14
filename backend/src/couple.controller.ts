import { Body, Controller, Get, Post, Query } from '@nestjs/common';
import { PrismaService } from './prisma.service';

@Controller('couple')
export class CoupleController {
  constructor(private prisma: PrismaService) {}

  @Get()
  async get(@Query('userId') userId: string) {
    const membership = await this.prisma.coupleMember.findFirst({
      where: { userId },
      include: { couple: true }
    });
    return membership?.couple || null;
  }

  // Semua id user dalam couple yang sama dengan userId (fallback: diri sendiri saja)
  private async getCoupleMemberIds(userId: string): Promise<string[]> {
    const membership = await this.prisma.coupleMember.findFirst({ where: { userId } });
    if (!membership) return [userId];
    const members = await this.prisma.coupleMember.findMany({ where: { coupleId: membership.coupleId } });
    return members.map((m) => m.userId);
  }

  // Total saldo + total pemasukan + total pengeluaran gabungan kedua user (semua visibility)
  @Get('summary')
  async summary(@Query('userId') userId: string) {
    const memberIds = await this.getCoupleMemberIds(userId);

    const accounts = await this.prisma.account.findMany({
      where: { userId: { in: memberIds }, isActive: true }
    });
    const totalBalance = accounts.reduce((sum, a) => sum + Number(a.balance), 0);

    const [incomeAgg, expenseAgg] = await Promise.all([
      this.prisma.transaction.aggregate({
        where: { userId: { in: memberIds }, type: 'INCOME' },
        _sum: { amount: true }
      }),
      this.prisma.transaction.aggregate({
        where: { userId: { in: memberIds }, type: 'EXPENSE' },
        _sum: { amount: true }
      })
    ]);

    return {
      totalBalance,
      totalIncome: Number(incomeAgg._sum.amount || 0),
      totalExpense: Number(expenseAgg._sum.amount || 0)
    };
  }

  // Semua rekening milik kedua user, lengkap dengan info pemilik
  @Get('accounts')
  async coupleAccounts(@Query('userId') userId: string) {
    const memberIds = await this.getCoupleMemberIds(userId);
    return this.prisma.account.findMany({
      where: { userId: { in: memberIds }, isActive: true },
      include: { user: { select: { id: true, displayName: true } } },
      orderBy: { createdAt: 'asc' }
    });
  }

  // Semua transaksi milik kedua user, lengkap dengan info pemilik
  @Get('transactions')
  async coupleTransactions(@Query('userId') userId: string) {
    const memberIds = await this.getCoupleMemberIds(userId);
    return this.prisma.transaction.findMany({
      where: { userId: { in: memberIds } },
      include: {
        account: true,
        category: true,
        user: { select: { id: true, displayName: true } }
      },
      orderBy: { transactionDate: 'desc' },
      take: 100
    });
  }

  @Get('messages')
  async messages(@Query('userId') userId: string) {
    const membership = await this.prisma.coupleMember.findFirst({ where: { userId } });
    if (!membership) return [];
    const conversation = await this.prisma.conversation.findFirst({ where: { coupleId: membership.coupleId } });
    if (!conversation) return [];
    return this.prisma.message.findMany({
      where: { conversationId: conversation.id },
      include: { sender: { select: { id: true, displayName: true, username: true } } },
      orderBy: { createdAt: 'asc' },
      take: 200
    });
  }

  @Post('messages')
  async send(@Body() body: { userId: string; message: string }) {
    const membership = await this.prisma.coupleMember.findFirst({ where: { userId: body.userId } });
    if (!membership) throw new Error('Couple tidak ditemukan');
    const conversation = await this.prisma.conversation.findFirst({ where: { coupleId: membership.coupleId } });
    if (!conversation) throw new Error('Conversation tidak ditemukan');
    return this.prisma.message.create({
      data: { conversationId: conversation.id, senderId: body.userId, message: body.message },
      include: { sender: { select: { id: true, displayName: true } } }
    });
  }

  // ===== FITUR BARU DI BAWAH INI — endpoint lama di atas tidak diubah =====

  // scope: 'me' | 'partner' | 'both' (default 'both'), dipakai oleh /report dan /cashflow
  private async resolveScopeIds(userId: string, scope?: string): Promise<string[]> {
    const memberIds = await this.getCoupleMemberIds(userId);
    if (scope === 'me') return [userId];
    if (scope === 'partner') return memberIds.filter((id) => id !== userId);
    return memberIds;
  }

  // Laporan bulanan: total income/expense/net cash flow/saldo + breakdown per kategori.
  // Filter Saya / Pasangan / Bersama via query param scope.
  @Get('report')
  async report(
    @Query('userId') userId: string,
    @Query('year') year?: string,
    @Query('month') month?: string,
    @Query('scope') scope?: string
  ) {
    const now = new Date();
    const y = year ? parseInt(year, 10) : now.getFullYear();
    const m = month ? parseInt(month, 10) : now.getMonth() + 1; // 1-12
    const start = new Date(Date.UTC(y, m - 1, 1));
    const end = new Date(Date.UTC(y, m, 1));

    const scopeIds = await this.resolveScopeIds(userId, scope);

    const [incomeTx, expenseTx, accounts] = await Promise.all([
      this.prisma.transaction.findMany({
        where: { userId: { in: scopeIds }, type: 'INCOME', transactionDate: { gte: start, lt: end } },
        include: { category: true }
      }),
      this.prisma.transaction.findMany({
        where: { userId: { in: scopeIds }, type: 'EXPENSE', transactionDate: { gte: start, lt: end } },
        include: { category: true }
      }),
      this.prisma.account.findMany({ where: { userId: { in: scopeIds }, isActive: true } })
    ]);

    const totalIncome = incomeTx.reduce((sum, t) => sum + Number(t.amount), 0);
    const totalExpense = expenseTx.reduce((sum, t) => sum + Number(t.amount), 0);
    const totalBalance = accounts.reduce((sum, a) => sum + Number(a.balance), 0);

    const groupByCategory = (rows: typeof incomeTx) => {
      const map = new Map<string, number>();
      for (const t of rows) {
        const name = t.category?.name || 'Tanpa Kategori';
        map.set(name, (map.get(name) || 0) + Number(t.amount));
      }
      return Array.from(map.entries()).map(([category, total]) => ({ category, total }));
    };

    return {
      period: `${y}-${String(m).padStart(2, '0')}`,
      scope: scope || 'both',
      totalIncome,
      totalExpense,
      netCashFlow: totalIncome - totalExpense,
      totalBalance,
      transactionCount: incomeTx.length + expenseTx.length,
      incomeByCategory: groupByCategory(incomeTx),
      expenseByCategory: groupByCategory(expenseTx)
    };
  }

  // Data untuk line chart cash flow bulanan (income vs expense), N bulan terakhir.
  @Get('cashflow')
  async cashflow(
    @Query('userId') userId: string,
    @Query('scope') scope?: string,
    @Query('months') monthsParam?: string
  ) {
    const months = Math.min(Math.max(parseInt(monthsParam || '6', 10) || 6, 1), 24);
    const scopeIds = await this.resolveScopeIds(userId, scope);
    const now = new Date();
    const points: { month: string; income: number; expense: number }[] = [];

    for (let i = months - 1; i >= 0; i--) {
      const start = new Date(Date.UTC(now.getFullYear(), now.getMonth() - i, 1));
      const end = new Date(Date.UTC(start.getUTCFullYear(), start.getUTCMonth() + 1, 1));

      const [incomeAgg, expenseAgg] = await Promise.all([
        this.prisma.transaction.aggregate({
          where: { userId: { in: scopeIds }, type: 'INCOME', transactionDate: { gte: start, lt: end } },
          _sum: { amount: true }
        }),
        this.prisma.transaction.aggregate({
          where: { userId: { in: scopeIds }, type: 'EXPENSE', transactionDate: { gte: start, lt: end } },
          _sum: { amount: true }
        })
      ]);

      points.push({
        month: `${start.getUTCFullYear()}-${String(start.getUTCMonth() + 1).padStart(2, '0')}`,
        income: Number(incomeAgg._sum.amount || 0),
        expense: Number(expenseAgg._sum.amount || 0)
      });
    }

    return points;
  }
}
