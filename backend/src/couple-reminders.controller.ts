import { Body, Controller, Delete, Get, Param, Post, Put, Query } from '@nestjs/common';
import { PrismaService } from './prisma.service';

@Controller('couple/reminders')
export class CoupleRemindersController {
  constructor(private prisma: PrismaService) {}

  private async getCoupleId(userId: string): Promise<string | null> {
    const membership = await this.prisma.coupleMember.findFirst({ where: { userId } });
    return membership?.coupleId || null;
  }

  @Get()
  async list(@Query('userId') userId: string) {
    const coupleId = await this.getCoupleId(userId);
    if (!coupleId) return [];
    return this.prisma.coupleReminder.findMany({
      where: { coupleId },
      include: { creator: { select: { id: true, displayName: true } } },
      orderBy: { reminderAt: 'asc' }
    });
  }

  @Post()
  async create(@Body() body: { userId: string; title: string; description?: string; reminderAt: string }) {
    const coupleId = await this.getCoupleId(body.userId);
    if (!coupleId) throw new Error('Couple tidak ditemukan');
    return this.prisma.coupleReminder.create({
      data: {
        coupleId,
        createdBy: body.userId,
        title: body.title,
        description: body.description || null,
        reminderAt: new Date(body.reminderAt)
      },
      include: { creator: { select: { id: true, displayName: true } } }
    });
  }

  @Put(':id/toggle')
  async toggle(@Param('id') id: string) {
    const reminder = await this.prisma.coupleReminder.findUnique({ where: { id } });
    if (!reminder) throw new Error('Pengingat tidak ditemukan');
    return this.prisma.coupleReminder.update({
      where: { id },
      data: { isCompleted: !reminder.isCompleted },
      include: { creator: { select: { id: true, displayName: true } } }
    });
  }

  @Delete(':id')
  async remove(@Param('id') id: string) {
    await this.prisma.coupleReminder.delete({ where: { id } });
    return { deleted: true };
  }
}
