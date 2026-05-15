import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Layout } from '@/components/common/Layout';
import { ProtectedRoute } from '@/components/common/ProtectedRoute';
import { LoginPage } from '@/features/auth/LoginPage';
import { HomePage } from '@/features/home/HomePage';
import { DashboardPage } from '@/features/dashboard/DashboardPage';
import {
  AnnouncementsPage,
  AnnouncementDetailPage,
  TasksPage,
  TaskDetailPage,
  MessagesPage,
  ForumPage,
  ForumCategoryPage,
  ForumTopicPage,
  DocumentsPage,
  DuesPage,
  PollsPage,
  PollDetailPage,
  ProfilePage,
  SettingsPage,
  NotificationsPage,
  ForgotPasswordPage,
  ResetPasswordPage,
  ApplyPage,
  NotFoundPage,
  AdminUsersPage,
  AdminPropertiesPage,
  AdminAnnouncementNewPage,
  AdminTasksPage,
  AdminDuesImportPage,
  AdminPollNewPage,
  AdminInvitePage,
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
          <Route path="/forum/:categorySlug" element={<ForumCategoryPage />} />
          <Route path="/forum/topics/:id" element={<ForumTopicPage />} />

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
                <DashboardPage />
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
                <AdminUsersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/properties"
            element={
              <ProtectedRoute adminOnly>
                <AdminPropertiesPage />
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
                <AdminInvitePage />
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
