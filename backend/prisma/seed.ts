import { PrismaClient, UserRole, AccountType, TransactionType, VisibilityType } from '@prisma/client';
import * as bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

async function main() {
  const chaiPassword = process.env.CHAI_PASSWORD || 'chaisayangnurma';
  const nurmaPassword = process.env.NURMA_PASSWORD || 'nurmasayangchai';

  const chaiHash = await bcrypt.hash(chaiPassword, 12);
  const nurmaHash = await bcrypt.hash(nurmaPassword, 12);

  const chai = await prisma.user.upsert({
    where: { username: 'chai' },
    update: { displayName: 'Chairul', passwordHash: chaiHash, role: UserRole.ADMIN, isActive: true },
    create: { username: 'chai', displayName: 'Chairul', passwordHash: chaiHash, role: UserRole.ADMIN }
  });

  const nurma = await prisma.user.upsert({
    where: { username: 'nurma' },
    update: { displayName: 'Nurma', passwordHash: nurmaHash, role: UserRole.ADMIN, isActive: true },
    create: { username: 'nurma', displayName: 'Nurma', passwordHash: nurmaHash, role: UserRole.ADMIN }
  });

  let couple = await prisma.couple.findFirst();
  if (!couple) {
    couple = await prisma.couple.create({ data: { name: 'Chai ❤️ Nurma' } });
  }

  await prisma.coupleMember.upsert({
    where: { coupleId_userId: { coupleId: couple.id, userId: chai.id } },
    update: {},
    create: { coupleId: couple.id, userId: chai.id }
  });

  await prisma.coupleMember.upsert({
    where: { coupleId_userId: { coupleId: couple.id, userId: nurma.id } },
    update: {},
    create: { coupleId: couple.id, userId: nurma.id }
  });

  const accounts = [
    { userId: chai.id, name: 'Cash Chai', type: AccountType.CASH },
    { userId: nurma.id, name: 'Cash Nurma', type: AccountType.CASH }
  ];

  for (const a of accounts) {
    const existing = await prisma.account.findFirst({ where: { userId: a.userId, name: a.name } });
    if (!existing) await prisma.account.create({ data: a });
  }

  const categories = [
    ['Makanan', TransactionType.EXPENSE],
    ['Transportasi', TransactionType.EXPENSE],
    ['Belanja', TransactionType.EXPENSE],
    ['Tagihan', TransactionType.EXPENSE],
    ['Hiburan', TransactionType.EXPENSE],
    ['Gaji', TransactionType.INCOME],
    ['Bonus', TransactionType.INCOME],
    ['Lainnya', TransactionType.EXPENSE]
  ] as const;

  for (const [name, type] of categories) {
    const existing = await prisma.category.findFirst({ where: { userId: chai.id, name, transactionType: type } });
    if (!existing) await prisma.category.create({ data: { userId: chai.id, name, transactionType: type, visibility: VisibilityType.SHARED } });
  }

  let conversation = await prisma.conversation.findFirst({ where: { coupleId: couple.id } });
  if (!conversation) {
    conversation = await prisma.conversation.create({ data: { coupleId: couple.id } });
    await prisma.conversationMember.createMany({
      data: [
        { conversationId: conversation.id, userId: chai.id },
        { conversationId: conversation.id, userId: nurma.id }
      ],
      skipDuplicates: true
    });
  }

  console.log('Seed selesai: Chai & Nurma dibuat sebagai ADMIN.');
}

main()
  .catch(async (e) => { console.error(e); process.exit(1); })
  .finally(async () => prisma.$disconnect());
