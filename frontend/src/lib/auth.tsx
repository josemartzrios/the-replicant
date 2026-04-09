"use client";

/**
 * Auth Context for The Replicant.
 *
 * Provides authentication state management across the app:
 * - Login / Setup / Logout flows
 * - User state in localStorage (non-sensitive: name, email, role only)
 * - Tokens handled exclusively via httpOnly cookies (set by backend)
 * - Auto-redirect on 401 handled in api.ts
 */

import {
    createContext,
    useContext,
    useState,
    useEffect,
    useCallback,
    type ReactNode,
} from "react";
import { api } from "./api";
import type { UserDTO, AuthResponse } from "./types";

interface AuthState {
    user: UserDTO | null;
    isAuthenticated: boolean;
    isLoading: boolean;
}

interface AuthContextType extends AuthState {
    login: (email: string, password: string) => Promise<void>;
    setup: (email: string, name: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function saveAuthData(response: AuthResponse): void {
    // Only the non-sensitive user profile is stored in localStorage.
    // Tokens are managed exclusively by the backend via httpOnly cookies.
    localStorage.setItem("user", JSON.stringify(response.user));
    // Set a routing hint cookie on the Vercel domain so the Next.js middleware
    // can gate /admin/* routes server-side. This cookie holds no sensitive data.
    document.cookie = "session=1; path=/; SameSite=Lax; Secure; Max-Age=86400";
}

function clearAuthData(): void {
    localStorage.removeItem("user");
    document.cookie = "session=; path=/; SameSite=Lax; Secure; Max-Age=0";
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [state, setState] = useState<AuthState>({
        user: null,
        isAuthenticated: false,
        isLoading: true,
    });

    // Restore user profile from localStorage on mount.
    // Actual session validity is enforced by the backend on each API call
    // via the httpOnly accessToken cookie. A 401 response clears this state
    // and redirects to /login (handled in api.ts).
    useEffect(() => {
        const userStr = localStorage.getItem("user");
        if (userStr) {
            try {
                const user: UserDTO = JSON.parse(userStr);
                setState({ user, isAuthenticated: true, isLoading: false });
            } catch {
                clearAuthData();
                setState({ user: null, isAuthenticated: false, isLoading: false });
            }
        } else {
            setState({ user: null, isAuthenticated: false, isLoading: false });
        }
    }, []);

    const handleAuthResponse = useCallback((response: AuthResponse) => {
        saveAuthData(response);
        setState({
            user: response.user,
            isAuthenticated: true,
            isLoading: false,
        });
    }, []);

    const login = useCallback(
        async (email: string, password: string) => {
            const response = await api.login({ email, password });
            handleAuthResponse(response);
        },
        [handleAuthResponse]
    );

    const setup = useCallback(
        async (email: string, name: string, password: string) => {
            const response = await api.setup({ email, name, password });
            handleAuthResponse(response);
        },
        [handleAuthResponse]
    );

    const logout = useCallback(async () => {
        try {
            await api.logout();
        } catch {
            // Logout should always succeed on the client side
        } finally {
            clearAuthData();
            setState({ user: null, isAuthenticated: false, isLoading: false });
            window.location.href = "/login";
        }
    }, []);

    return (
        <AuthContext.Provider value={{ ...state, login, setup, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth(): AuthContextType {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
}
