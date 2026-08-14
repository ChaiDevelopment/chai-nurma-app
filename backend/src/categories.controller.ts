import { Controller, Get, Query } from '@nestjs/common';
import { PrismaService } from './prisma.service';

// Endpoint BARU. Tidak menggantikan endpoint lain yang sudah ada.
@Controller('categories')
export class CategoriesController {
  constructor(private prisma: PrismaService) {}

  // Kategori milik user (PRIVATE) + kategori SHARED yang bisa dipakai semua user dalam couple.
  @Get()
  async list(@Query('userId') userId: string) {
    return this.prisma.category.findMany({
      where: {
        OR: [{ userId }, { visibility: 'SHARED' }]
      },
      orderBy: [{ transactionType: 'asc' }, { name: 'asc' }]
    });
  }
}
