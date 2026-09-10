-- ============================================================
-- Supabase 数据库结构（web3-blog）
-- 用途：博客文章 / 音乐馆歌曲 / 用户资料 / 示例业务表
-- 用法：登录 Supabase → SQL Editor → 粘贴本文件执行
--       或：supabase db push
-- 说明：数据通过 Row Level Security 保护，前端用 publishable key
--       只能读（RLS 开放 SELECT），写操作提示需登录/服务端。
-- ============================================================

-- ------------------------------------------------------------
-- 1) 用户资料 profiles：与 Supabase Auth 的 auth.users 关联
-- ------------------------------------------------------------
create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  username text unique,
  full_name text,
  avatar_url text,
  bio text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "profiles_public_read" on public.profiles
  for select using (true);
create policy "profiles_self_update" on public.profiles
  for update using (auth.uid() = id);
create policy "profiles_self_insert" on public.profiles
  for insert with check (auth.uid() = id);

-- 新用户注册后自动建 profile 的触发器
create or replace function public.handle_new_user()
returns trigger
language plpgsql security definer set search_path = public
as $$
begin
  insert into public.profiles (id, username, full_name, avatar_url)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'username', split_part(new.email, '@', 1)),
    new.raw_user_meta_data->>'full_name',
    new.raw_user_meta_data->>'avatar_url'
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- ------------------------------------------------------------
-- 2) 博客文章 blog_articles
-- ------------------------------------------------------------
create table if not exists public.blog_articles (
  id bigint generated always as identity primary key,
  title text not null,
  slug text unique,
  summary text,
  content text,
  tags text[] default '{}',
  category text,
  author_id uuid references public.profiles (id) on delete set null,
  author_name text,
  cover_url text,
  views integer default 0,
  likes integer default 0,
  published boolean default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.blog_articles enable row level security;

create policy "blog_articles_public_read" on public.blog_articles
  for select using (published = true or auth.uid() = author_id);
create policy "blog_articles_insert" on public.blog_articles
  for insert with check (auth.uid() = author_id);
create policy "blog_articles_update" on public.blog_articles
  for update using (auth.uid() = author_id);

-- ------------------------------------------------------------
-- 3) 音乐馆歌曲 music_tracks
-- ------------------------------------------------------------
create table if not exists public.music_tracks (
  id bigint generated always as identity primary key,
  title text not null,
  artist text,
  album text,
  cover_url text,
  audio_url text,
  duration_seconds integer default 0,
  category text,
  tags text[] default '{}',
  netease_id bigint,
  play_count integer default 0,
  created_at timestamptz not null default now()
);

alter table public.music_tracks enable row level security;

create policy "music_tracks_public_read" on public.music_tracks
  for select using (true);
create policy "music_tracks_insert" on public.music_tracks
  for insert with check (auth.uid() is not null);

-- ------------------------------------------------------------
-- 4) 示例业务表 demo_items（演示读取通路，可任意扩展）
-- ------------------------------------------------------------
create table if not exists public.demo_items (
  id bigint generated always as identity primary key,
  name text not null,
  description text,
  category text,
  meta jsonb default '{}'::jsonb,
  created_at timestamptz not null default now()
);

alter table public.demo_items enable row level security;
create policy "demo_items_public_read" on public.demo_items
  for select using (true);
