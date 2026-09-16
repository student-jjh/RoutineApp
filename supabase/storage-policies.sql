-- Run once in Supabase Dashboard → SQL Editor.
-- Create a private Storage bucket named `routine-backups` first.
insert into storage.buckets (id, name, public)
values ('routine-backups', 'routine-backups', false)
on conflict (id) do update set public = false;

drop policy if exists "Users can upload their Routive backup" on storage.objects;
create policy "Users can upload their Routive backup"
on storage.objects for insert to authenticated
with check (
  bucket_id = 'routine-backups'
  and name = (auth.uid()::text || '/latest.json')
);

drop policy if exists "Users can replace their Routive backup" on storage.objects;
create policy "Users can replace their Routive backup"
on storage.objects for update to authenticated
using (
  bucket_id = 'routine-backups'
  and name = (auth.uid()::text || '/latest.json')
)
with check (
  bucket_id = 'routine-backups'
  and name = (auth.uid()::text || '/latest.json')
);

drop policy if exists "Users can download their Routive backup" on storage.objects;
create policy "Users can download their Routive backup"
on storage.objects for select to authenticated
using (
  bucket_id = 'routine-backups'
  and name = (auth.uid()::text || '/latest.json')
);

drop policy if exists "Users can delete their Routive backup" on storage.objects;
create policy "Users can delete their Routive backup"
on storage.objects for delete to authenticated
using (
  bucket_id = 'routine-backups'
  and name = (auth.uid()::text || '/latest.json')
);
