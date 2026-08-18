import { Body, Controller, Get, Post, Query, Req, UnauthorizedException, UseGuards } from '@nestjs/common';
import { PrismaService } from './prisma.service';
import { JwtAuthGuard } from './auth.guard';

interface AuthRequest {
  user: { sub: string; username: string; role: 'USER' | 'ADMIN' | 'MONITOR' };
}

interface UsageEntry {
  packageName: string;
  appName: string;
  usageDate: string;
  totalTimeMs: number;
  lastTimeUsed?: string | null;
}

@Controller('activity')
@UseGuards(JwtAuthGuard)
export class ActivityController {
  constructor(private prisma: PrismaService) {}

  @Post('sync')
  async sync(@Req() req: AuthRequest, @Body() body: { entries?: UsageEntry[] }) {
    if (req.user.role === 'MONITOR') {
      throw new UnauthorizedException('Akun monitor hanya untuk melihat aktivitas');
    }

    const entries = Array.isArray(body.entries) ? body.entries : [];
    let saved = 0;

    for (const entry of entries.slice(0, 500)) {
      if (!entry.packageName || !entry.appName || !entry.usageDate || !Number.isFinite(entry.totalTimeMs)) continue;
      const usageDate = new Date(`${entry.usageDate}T00:00:00.000Z`);
      if (Number.isNaN(usageDate.getTime())) continue;

      await this.prisma.usageActivity.upsert({
        where: {
          userId_packageName_usageDate: {
            userId: req.user.sub,
            packageName: entry.packageName,
            usageDate
          }
        },
        update: {
          appName: entry.appName.slice(0, 255),
          totalTimeMs: BigInt(Math.max(0, Math.round(entry.totalTimeMs))),
          lastTimeUsed: entry.lastTimeUsed ? new Date(entry.lastTimeUsed) : null
        },
        create: {
          userId: req.user.sub,
          packageName: entry.packageName.slice(0, 255),
          appName: entry.appName.slice(0, 255),
          usageDate,
          totalTimeMs: BigInt(Math.max(0, Math.round(entry.totalTimeMs))),
          lastTimeUsed: entry.lastTimeUsed ? new Date(entry.lastTimeUsed) : null
        }
      });
      saved++;
    }

    return { saved };
  }

  @Get('monitor')
  async monitor(@Req() req: AuthRequest, @Query('days') daysParam?: string) {
    if (req.user.role !== 'MONITOR') {
      throw new UnauthorizedException('Khusus akun monitor');
    }

    const days = Math.min(Math.max(parseInt(daysParam || '7', 10) || 7, 1), 31);
    const start = new Date();
    start.setUTCHours(0, 0, 0, 0);
    start.setUTCDate(start.getUTCDate() - (days - 1));

    const rows = await this.prisma.usageActivity.findMany({
      where: { usageDate: { gte: start } },
      include: { user: { select: { id: true, username: true, displayName: true } } },
      orderBy: [{ usageDate: 'desc' }, { totalTimeMs: 'desc' }]
    });

    return rows.map((row) => ({
      id: row.id,
      user: row.user,
      packageName: row.packageName,
      appName: row.appName,
      usageDate: row.usageDate.toISOString().slice(0, 10),
      totalTimeMs: row.totalTimeMs.toString(),
      lastTimeUsed: row.lastTimeUsed?.toISOString() ?? null
    }));
  }
}
