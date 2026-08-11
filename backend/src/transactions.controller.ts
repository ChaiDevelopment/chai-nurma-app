import { Body, Controller, Get, Post, Query } from '@nestjs/common';
import { PrismaService } from './prisma.service';

@Controller('transactions')
export class TransactionsController {
  constructor(private prisma: PrismaService) {}

  @Get()
  async list(@Query('userId') userId: string) {
    return this.prisma.transaction.findMany({
      where: { userId },
      include: { account: true, category: true },
      orderBy: { transactionDate: 'desc' },
      take: 100
    });
  }

  @Post()
  async create(@Body() body: {
    userId: string; accountId: string; categoryId?: string;
    type: 'INCOME'|'EXPENSE'; amount: number; description?: string;
    visibility?: 'PRIVATE'|'SHARED';
  }) {
    return this.prisma.$transaction(async (tx) => {
      const account = await tx.account.findUnique({ where: { id: body.accountId } });
      if (!account) throw new Error('Account tidak ditemukan');

      const amount = Number(body.amount);
      const newBalance = body.type === 'INCOME'
        ? Number(account.balance) + amount
        : Number(account.balance) - amount;

      if (newBalance < 0) throw new Error('Saldo tidak cukup');

      const result = await tx.transaction.create({
        data: {
          userId: body.userId,
          accountId: body.accountId,
          categoryId: body.categoryId,
          type: body.type,
          amount,
          description: body.description,
          visibility: body.visibility || 'PRIVATE'
        }
      });

      await tx.account.update({
        where: { id: body.accountId },
        data: { balance: newBalance }
      });

      return result;
    });
  }
}
