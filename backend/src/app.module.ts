import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PrismaService } from './prisma.service';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { AccountsController } from './accounts.controller';
import { TransactionsController } from './transactions.controller';
import { CoupleController } from './couple.controller';

@Module({
  imports: [JwtModule.register({ secret: process.env.JWT_SECRET || 'dev-only-change-me', signOptions: { expiresIn: '7d' } })],
  controllers: [AuthController, AccountsController, TransactionsController, CoupleController],
  providers: [PrismaService, AuthService],
})
export class AppModule {}
