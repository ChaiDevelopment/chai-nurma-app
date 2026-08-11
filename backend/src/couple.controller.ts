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
