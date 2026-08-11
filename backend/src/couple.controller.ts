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
}
