import { Body, Controller, Delete, Get, Param, Post, Put, Query } from '@nestjs/common';
import { PrismaService } from './prisma.service';

@Controller('budgets')
export class BudgetsController {
  constructor(private prisma: PrismaService) {}

  @Get()
  async list(@Query('userId') userId: string, @Query('month') month?: string) {
    const where: any = { userId };
    if (month) where.month = new Date(month);
    return this.prisma.budget.findMany({
      where,
      include: { category: true },
      orderBy: { month: 'desc' }
    });
  }

  @Post()
  async create(@Body() body: {
    userId: string; categoryId?: string; name: string; amount: number; month: string; visibility?: string;
  }) {
    return this.prisma.budget.create({
      data: {
        userId: body.userId,
        categoryId: body.categoryId || null,
        name: body.name,
        amount: body.amount,
        month: new Date(body.month),
        visibility: (body.visibility as any) || 'PRIVATE'
      },
      include: { category: true }
    });
  }

  @Put(':id')
  async update(@Param('id') id: string, @Body() body: { name?: string; amount?: number; categoryId?: string }) {
    return this.prisma.budget.update({
      where: { id },
      data: {
        ...(body.name !== undefined && { name: body.name }),
        ...(body.amount !== undefined && { amount: body.amount }),
        ...(body.categoryId !== undefined && { categoryId: body.categoryId })
      },
      include: { category: true }
    });
  }

  @Delete(':id')
  async remove(@Param('id') id: string) {
    await this.prisma.budget.delete({ where: { id } });
    return { deleted: true };
  }

  @Get('progress')
  async progress(@Query('userId') userId: string, @Query('month') month: string) {
    const start = new Date(month);
    const end = new Date(start.getFullYear(), start.getMonth() + 1, 1);

    const budgets = await this.prisma.budget.findMany({
      where: { userId, month: start },
      include: { category: true }
    });

    return Promise.all(budgets.map(async (b) => {
      const spent = await this.prisma.transaction.aggregate({
        where: {
          userId,
          type: 'EXPENSE',
          categoryId: b.categoryId,
          transactionDate: { gte: start, lt: end }
        },
        _sum: { amount: true }
      });
      const spentAmount = Number(spent._sum.amount || 0);
      const budgetAmount = Number(b.amount);
      return {
        id: b.id,
        name: b.name,
        category: b.category?.name || 'Umum',
        budgetAmount,
        spentAmount,
        remaining: budgetAmount - spentAmount,
        percentage: budgetAmount === 0 ? 0 : spentAmount / budgetAmount
      };
    }));
  }
}
