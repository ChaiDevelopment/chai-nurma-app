import { Body, Controller, Delete, Get, Param, Post, Put, Query } from '@nestjs/common';
import { PrismaService } from './prisma.service';

@Controller('couple/notes')
export class CoupleNotesController {
  constructor(private prisma: PrismaService) {}

  private async getCoupleId(userId: string): Promise<string | null> {
    const membership = await this.prisma.coupleMember.findFirst({ where: { userId } });
    return membership?.coupleId || null;
  }

  @Get()
  async list(@Query('userId') userId: string) {
    const coupleId = await this.getCoupleId(userId);
    if (!coupleId) return [];
    return this.prisma.coupleNote.findMany({
      where: { coupleId },
      include: { creator: { select: { id: true, displayName: true } } },
      orderBy: { updatedAt: 'desc' }
    });
  }

  @Post()
  async create(@Body() body: { userId: string; title: string; content?: string }) {
    const coupleId = await this.getCoupleId(body.userId);
    if (!coupleId) throw new Error('Couple tidak ditemukan');
    return this.prisma.coupleNote.create({
      data: { coupleId, createdBy: body.userId, title: body.title, content: body.content || null },
      include: { creator: { select: { id: true, displayName: true } } }
    });
  }

  @Put(':id')
  async update(@Param('id') id: string, @Body() body: { title?: string; content?: string }) {
    return this.prisma.coupleNote.update({
      where: { id },
      data: {
        ...(body.title !== undefined && { title: body.title }),
        ...(body.content !== undefined && { content: body.content })
      },
      include: { creator: { select: { id: true, displayName: true } } }
    });
  }

  @Delete(':id')
  async remove(@Param('id') id: string) {
    await this.prisma.coupleNote.delete({ where: { id } });
    return { deleted: true };
  }
}
