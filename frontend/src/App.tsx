import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Layout } from '@/components/common/Layout';
import { ProtectedRoute } from '@/components/common/ProtectedRoute';

import { HomePage } from '@/features/home/HomePage';
import { LoginPage } from '@/features/auth/LoginPage';
import { DashboardPage } from '@/features/dashboard/DashboardPage';

import { AnnouncementsPage } from '@/features/announcements/AnnouncementsPage';
import { AnnouncementDetailPage } from '@/features/announcements/AnnouncementDetailPage';

import { TasksPage } from '@/features/tasks/TasksPage';
import { TaskDetailPage } from '@/features/tasks/TaskDetailPage';

import { MessagesPage } from '@/features/messages/MessagesPage';

import { ForumPage } from '@/features/forum/ForumPage';

import { DocumentsPage } from '@/features/documents/DocumentsPage';

import { DuesPage } from '@/features/dues/DuesPage';

import { PollsPage } from '@/features/polls/PollsPage';
import { PollDetailPage } from '@/features/polls/PollDetailPage';

import { UsersPage } from '@/features/admin/users/UsersPage';
import { PropertiesPage } from '@/features/admin/properties/PropertiesPage';
import { InvitationsPage } from '@/features/admin/invitations/InvitationsPage';

import {
  ProfilePage,
  SettingsPage,
  NotificationsPage,
  ForgotPasswordPage,
  ResetPasswordPage,
  ApplyPage,
  NotFoundPage,
  AdminAnnouncementNewPage,
  AdminTasksPage,
  AdminDuesImportPage,
  AdminPollNewPage,
  AdminDashboardPage,
} from '@/features/_placeholders';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* ── Public routes ─────────────────────────────────────── */}
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/apply" element={<ApplyPage />} />

        {/* ── Protected routes (auth required) ──────────────────── */}
        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<DashboardPage />} />

          <Route path="/announcements" element={<AnnouncementsPage />} />
          <Route path="/announcements/:id" element={<AnnouncementDetailPage />} />

          <Route path="/tasks" element={<TasksPage />} />
          <Route path="/tasks/:id" element={<TaskDetailPage />} />

          <Route path="/messages" element={<MessagesPage />} />

          <Route path="/forum" element={<ForumPage />} />
          <Route path="/forum/:categorySlug" element={<ForumPage />} />
          <Route path="/forum/topics/:id" element={<ForumPage />} />

          <Route path="/documents" element={<DocumentsPage />} />

          <Route path="/dues" element={<DuesPage />} />

          <Route path="/polls" element={<PollsPage />} />
          <Route path="/polls/:id" element={<PollDetailPage />} />

          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />

          {/* ── Admin routes (YONETIM_KURULU or SUPER_ADMIN) ───── */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute adminOnly>
                <AdminDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/dashboard"
            element={
              <ProtectedRoute adminOnly>
                <AdminDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/users"
            element={
              <ProtectedRoute adminOnly>
                <UsersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/properties"
            element={
              <ProtectedRoute adminOnly>
                <PropertiesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/announcements/new"
            element={
              <ProtectedRoute adminOnly>
                <AdminAnnouncementNewPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/tasks"
            element={
              <ProtectedRoute adminOnly>
                <AdminTasksPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/dues/import"
            element={
              <ProtectedRoute adminOnly>
                <AdminDuesImportPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/polls/new"
            element={
              <ProtectedRoute adminOnly>
                <AdminPollNewPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/invite"
            element={
              <ProtectedRoute adminOnly>
                <InvitationsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/invitations"
            element={
              <ProtectedRoute adminOnly>
                <InvitationsPage />
              </ProtectedRoute>
            }
          />
        </Route>

        {/* ── Catch-all ─────────────────────────────────────────── */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
