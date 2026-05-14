import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { login } from '@/api/auth';
import { useAuthStore } from '@/stores/authStore';
import { getMe } from '@/api/auth';

const schema = z.object({
  email: z.string().email('Geçerli e-posta girin'),
  password: z.string().min(1, 'Şifre gerekli'),
});

type FormData = z.infer<typeof schema>;

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { setAuth } = useAuthStore();
  const from = (location.state as { from?: { pathname: string } })?.from?.pathname ?? '/dashboard';

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  async function onSubmit(data: FormData) {
    try {
      const tokens = await login(data);
      // Temporarily set token so getMe() can fire
      useAuthStore.getState().setTokens(tokens.accessToken, tokens.refreshToken);
      const user = await getMe();
      setAuth(user, tokens.accessToken, tokens.refreshToken);
      navigate(from, { replace: true });
    } catch {
      setError('root', { message: 'E-posta veya şifre hatalı' });
    }
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-sm border border-border w-full max-w-sm p-8">
        <div className="mb-8 text-center">
          <h1 className="font-serif text-2xl text-foreground">Ekoköy Portalı</h1>
          <p className="text-sm text-muted-foreground mt-1">Seferihisar · 94 Hane</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-foreground mb-1.5">E-posta</label>
            <input
              type="email"
              className="w-full px-3 py-2 border border-border rounded-lg text-sm bg-white text-foreground outline-none focus:border-primary transition-colors"
              placeholder="ornek@email.com"
              {...register('email')}
            />
            {errors.email && (
              <p className="text-xs text-destructive mt-1">{errors.email.message}</p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-foreground mb-1.5">Şifre</label>
            <input
              type="password"
              className="w-full px-3 py-2 border border-border rounded-lg text-sm bg-white text-foreground outline-none focus:border-primary transition-colors"
              placeholder="••••••••"
              {...register('password')}
            />
            {errors.password && (
              <p className="text-xs text-destructive mt-1">{errors.password.message}</p>
            )}
          </div>

          {errors.root && (
            <p className="text-xs text-destructive text-center">{errors.root.message}</p>
          )}

          <Button type="submit" className="w-full" disabled={isSubmitting}>
            {isSubmitting ? 'Giriş yapılıyor…' : 'Giriş Yap'}
          </Button>
        </form>

        <div className="mt-4 text-center">
          <Link
            to="/forgot-password"
            className="text-xs text-primary hover:underline"
          >
            Şifremi unuttum
          </Link>
        </div>
      </div>
    </div>
  );
}
