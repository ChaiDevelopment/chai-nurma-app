# Chai Nurma App ❤️

MVP: Financial Tracking + Couple App.

## Stack
- Android: Kotlin + Jetpack Compose
- Backend: NestJS + Prisma
- Database: Neon PostgreSQL 18
- Auth: JWT + bcryptjs
- Chat foundation: WebSocket-ready NestJS gateway

## Important
Do NOT put your Neon password in source code or Git.
Create `backend/.env` locally.

Example:
DATABASE_URL="postgresql://USER:PASSWORD@HOST/DATABASE?sslmode=require&channel_binding=require"
JWT_SECRET="change-this-to-a-long-random-secret"

## Backend
```bash
cd backend
npm install
npx prisma generate
npm run prisma:push
npm run seed
npm run start:dev
```

API defaults to:
http://10.0.2.2:3000/api

For a physical Android phone, replace the API base URL in:
android/app/src/main/java/com/chai/nurma/app/data/ApiConfig.kt
with your PC LAN IP, e.g. http://192.168.1.10:3000/api/

## Initial accounts
Both are ADMIN:
- chai / (the password you supplied in chat)
- nurma / (the password you supplied in chat)

The seed hashes passwords with bcryptjs at runtime. Passwords are NOT stored plaintext.

## Android
Open the `android` directory in Android Studio and let Gradle sync.
Then run the `app` configuration.

The first Android screen is Login. After login it opens the financial dashboard.
