import { Body, Controller, Get, Post, Query } from '@nestjs/common';
import { PrismaService } from './prisma.service';

// Endpoint BARU untuk fitur transfer antar rekening.
// Transfer BUKAN income/expense: tidak menyentuh table transactions,
// dan tidak masuk perhitungan income/expense pada endpoint manapun.
@Controller('transfers')
export class TransfersController {
  constructor(private prisma: PrismaService) {}

  @Get()
  async list(@Query('userId') userId: string) {
    return this.prisma.transfer.findMany({
      where: { userId },
      include: { fromAccount: true, toAccount: true },
      orderBy: { transferDate: 'desc' },
      take: 100
    });
  }

  @Post()
  async create(
    @Body()
    body: {
      userId: string;
      fromAccountId: string;
      toAccountId: string;
      amount: number;
      description?: string;
    }
  ) {
    if (body.fromAccountId === body.toAccountId) {
      throw new Error('Rekening asal dan tujuan tidak boleh sama');
    }

    return this.prisma.$transaction(async (tx) => {
      const from = await tx.account.findUnique({ where: { id: body.fromAccountId } });
      const to = await tx.account.findUnique({ where: { id: body.toAccountId } });
      if (!from || !to) throw new Error('Rekening tidak ditemukan');

      const amount = Number(body.amount);
      const newFromBalance = Number(from.balance) - amount;
      if (newFromBalance < 0) throw new Error('Saldo tidak cukup untuk transfer');

      const result = await tx.transfer.create({
        data: {
          userId: body.userId,
          fromAccountId: body.fromAccountId,
          toAccountId: body.toAccountId,
          amount,
          description: body.description
        },
        include: { fromAccount: true, toAccount: true }
      });

      await tx.account.update({
        where: { id: body.fromAccountId },
        data: { balance: newFromBalance }
      });

      await tx.account.update({
        where: { id: body.toAccountId },
        data: { balance: Number(to.balance) + amount }
      });

      return result;
    });
  }
}
