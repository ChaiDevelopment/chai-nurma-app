import { Body, Controller, Get, Post, Query } from '@nestjs/common';
import { PrismaService } from './prisma.service';

@Controller('accounts')
export class AccountsController {
  constructor(private prisma: PrismaService) {}

  @Get()
  async list(@Query('userId') userId: string) {
    return this.prisma.account.findMany({ where: { userId, isActive: true }, orderBy: { createdAt: 'asc' } });
  }

  @Post()
  async create(@Body() body: { userId: string; name: string; type: any; visibility?: any; balance?: number }) {
    return this.prisma.account.create({
      data: {
        userId: body.userId,
        name: body.name,
        type: body.type,
        visibility: body.visibility || 'PRIVATE',
        balance: body.balance || 0
      }
    });
  }
}
