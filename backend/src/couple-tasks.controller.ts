import { Body, Controller, Delete, Get, Param, Post, Put, Query } from '@nestjs/common';
import { PrismaService } from './prisma.service';

@Controller('couple/tasks')
export class CoupleTasksController {
  constructor(private prisma: PrismaService) {}

  private async getCoupleId(userId: string): Promise<string | null> {
    const membership = await this.prisma.coupleMember.findFirst({ where: { userId } });
    return membership?.coupleId || null;
  }

  @Get()
  async list(@Query('userId') userId: string) {
    const coupleId = await this.getCoupleId(userId);
    if (!coupleId) return [];
    return this.prisma.coupleTask.findMany({
      where: { coupleId },
      include: {
        creator: { select: { id: true, displayName: true } },
        assignee: { select: { id: true, displayName: true } }
      },
      orderBy: [{ isCompleted: 'asc' }, { dueDate: 'asc' }]
    });
  }

  @Post()
  async create(@Body() body: {
    userId: string; title: string; description?: string; assignedTo?: string; dueDate?: string;
  }) {
    const coupleId = await this.getCoupleId(body.userId);
    if (!coupleId) throw new Error('Couple tidak ditemukan');
    return this.prisma.coupleTask.create({
      data: {
        coupleId,
        createdBy: body.userId,
        title: body.title,
        description: body.description || null,
        assignedTo: body.assignedTo || null,
        dueDate: body.dueDate ? new Date(body.dueDate) : null
      },
      include: {
        creator: { select: { id: true, displayName: true } },
        assignee: { select: { id: true, displayName: true } }
      }
    });
  }

  @Put(':id/toggle')
  async toggle(@Param('id') id: string) {
    const task = await this.prisma.coupleTask.findUnique({ where: { id } });
    if (!task) throw new Error('Tugas tidak ditemukan');
    return this.prisma.coupleTask.update({
      where: { id },
      data: { isCompleted: !task.isCompleted },
      include: {
        creator: { select: { id: true, displayName: true } },
        assignee: { select: { id: true, displayName: true } }
      }
    });
  }

  @Delete(':id')
  async remove(@Param('id') id: string) {
    await this.prisma.coupleTask.delete({ where: { id } });
    return { deleted: true };
  }
}
