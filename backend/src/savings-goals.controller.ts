import { Body, Controller, Delete, Get, Param, Post, Query } from '@nestjs/common';
import { PrismaService } from './prisma.service';

@Controller('savings-goals')
export class SavingsGoalsController {
  constructor(private prisma: PrismaService) {}

  @Get()
  async list(@Query('userId') userId: string) {
    return this.prisma.savingsGoal.findMany({
      where: { userId },
      orderBy: { createdAt: 'desc' }
    });
  }

  @Post()
  async create(@Body() body: {
    userId: string; name: string; targetAmount: number; targetDate?: string; visibility?: string;
  }) {
    return this.prisma.savingsGoal.create({
      data: {
        userId: body.userId,
        name: body.name,
        targetAmount: body.targetAmount,
        currentAmount: 0,
        targetDate: body.targetDate ? new Date(body.targetDate) : null,
        visibility: (body.visibility as any) || 'PRIVATE'
      }
    });
  }

  @Post(':id/deposit')
  async deposit(@Param('id') id: string, @Body() body: { amount: number }) {
    const goal = await this.prisma.savingsGoal.findUnique({ where: { id } });
    if (!goal) throw new Error('Goal tidak ditemukan');
    const newAmount = Number(goal.currentAmount) + body.amount;
    return this.prisma.savingsGoal.update({
      where: { id },
      data: { currentAmount: newAmount }
    });
  }

  @Delete(':id')
  async remove(@Param('id') id: string) {
    await this.prisma.savingsGoal.delete({ where: { id } });
    return { deleted: true };
  }
}
