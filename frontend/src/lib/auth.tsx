"use client";

/**
 * Auth Context for The Replicant.
 *
 * Provides authentication state management across the app:
 * - Login / Setup / Logout flows
 * - Token persistence in localStorage
 * - Auto-refresh before token expiry
 * - User state available via useAuth() hook
 *
 * Security: Tokens in localStorage for MVP.
 * Production: migrate to httpOnly cookies.
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
    // Note: accessToken is handled by HttpOnly cookies now
    localStorage.setItem("refreshToken", response.refreshToken);
    localStorage.setItem("user", JSON.stringify(response.user));
}

function clearAuthData(): void {
    // Note: accessToken is handled by HttpOnly cookies now
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [state, setState] = useState<AuthState>({
        user: null,
        isAuthenticated: false,
        isLoading: true,
    });

    // Restore auth state from localStorage on mount
    useEffect(() => {
        // We no longer rely on 'accessToken' resting in localStorage.
        // As long as we have a refresh token and user info, we assume session is valid. 
        // The backend will enforce security via HttpOnly cookies.
        const refreshToken = localStorage.getItem("refreshToken");
        const userStr = localStorage.getItem("user");

        if (refreshToken && userStr) {
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
        const refreshToken = localStorage.getItem("refreshToken");
        try {
            if (refreshToken) {
                await api.logout(refreshToken);
            }
        } catch {
            // Logout should always succeed on the client side
        } finally {
            clearAuthData();
            setState({ user: null, isAuthenticated: false, isLoading: false });
            // Force a hard redirect to clear all React/Next.js client cache
            window.location.href = "/login";
        }
    }, []);

    return (
        <AuthContext.Provider
            value={{
                ...state,
                login,
                setup,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

/**
 * Hook to access authentication state and actions.
 *
 * @example
 * const { user, isAuthenticated, login, logout } = useAuth();
 */
export function useAuth(): AuthContextType {
    const context = useContext(AuthContext);

    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }

    return context;
}
