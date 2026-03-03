/**
 * API Client for The Replicant backend.
 *
 * Centralized fetch wrapper with:
 * - JWT token injection from localStorage
 * - Automatic 401 handling (clear token, redirect to login)
 * - Typed responses matching backend DTOs
 * - RFC 7807 error parsing
 *
 * Security: Tokens stored in localStorage (MVP).
 * Production should use httpOnly cookies.
 */

import type {
    ApiResponse,
    PaginatedResponse,
    AuthResponse,
    AuthStatus,
    LoginRequest,
    SetupRequest,
    PostDTO,
    CategoryDTO,
    TagDTO,
    CreatePostRequest,
    UpdatePostRequest,
    CreateCategoryRequest,
    ApiError,
} from "./types";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

// ============================================================
// Core Fetch
// ============================================================

class ApiClient {
    private getToken(): string | null {
        if (typeof window === "undefined") return null;
        return localStorage.getItem("accessToken");
    }

    private async request<T>(
        path: string,
        options: RequestInit = {}
    ): Promise<T> {
        const token = this.getToken();

        const headers: Record<string, string> = {
            "Content-Type": "application/json",
            ...((options.headers as Record<string, string>) || {}),
        };

        if (token) {
            headers["Authorization"] = `Bearer ${token}`;
        }

        const response = await fetch(`${API_URL}${path}`, {
            ...options,
            headers,
        });

        // Handle 204 No Content (e.g., DELETE)
        if (response.status === 204) {
            return undefined as T;
        }

        // Handle error responses
        if (!response.ok) {
            const error: ApiError = await response.json().catch(() => ({
                type: "about:blank",
                title: "Network Error",
                status: response.status,
                detail: "An unexpected error occurred",
            }));

            // On 401, clear token and redirect to login
            // Skip redirect for auth endpoints (login, setup, etc.)
            // so they can handle the error and show a message inline
            const isAuthEndpoint = path.startsWith("/auth/");
            if (response.status === 401 && !isAuthEndpoint && typeof window !== "undefined") {
                localStorage.removeItem("accessToken");
                localStorage.removeItem("refreshToken");
                localStorage.removeItem("user");
                window.location.href = "/login";
            }

            throw error;
        }

        return response.json();
    }

    // ============================================================
    // Auth
    // ============================================================

    async getAuthStatus(): Promise<AuthStatus> {
        return this.request<AuthStatus>("/auth/status");
    }

    async setup(data: SetupRequest): Promise<AuthResponse> {
        return this.request<AuthResponse>("/auth/setup", {
            method: "POST",
            body: JSON.stringify(data),
        });
    }

    async login(data: LoginRequest): Promise<AuthResponse> {
        return this.request<AuthResponse>("/auth/login", {
            method: "POST",
            body: JSON.stringify(data),
        });
    }

    async logout(refreshToken: string): Promise<void> {
        return this.request<void>("/auth/logout", {
            method: "POST",
            body: JSON.stringify({ refreshToken }),
        });
    }

    async refreshToken(refreshToken: string): Promise<AuthResponse> {
        return this.request<AuthResponse>("/auth/refresh", {
            method: "POST",
            body: JSON.stringify({ refreshToken }),
        });
    }

    // ============================================================
    // Posts (Public)
    // ============================================================

    async getPosts(
        page = 0,
        size = 10,
        category?: string,
        tag?: string
    ): Promise<PaginatedResponse<PostDTO>> {
        const params = new URLSearchParams({
            page: String(page),
            size: String(size),
        });
        if (category) params.set("category", category);
        if (tag) params.set("tag", tag);

        return this.request<PaginatedResponse<PostDTO>>(`/posts?${params}`);
    }

    async getPostBySlug(slug: string): Promise<ApiResponse<PostDTO>> {
        return this.request<ApiResponse<PostDTO>>(`/posts/${encodeURIComponent(slug)}`);
    }

    async searchPosts(
        query: string,
        page = 0,
        size = 10
    ): Promise<PaginatedResponse<PostDTO>> {
        const params = new URLSearchParams({
            q: query,
            page: String(page),
            size: String(size),
        });
        return this.request<PaginatedResponse<PostDTO>>(`/posts/search?${params}`);
    }

    // ============================================================
    // Posts (Admin)
    // ============================================================

    async getAdminPosts(
        page = 0,
        size = 10,
        status?: string
    ): Promise<PaginatedResponse<PostDTO>> {
        const params = new URLSearchParams({
            page: String(page),
            size: String(size),
        });
        if (status) params.set("status", status);

        return this.request<PaginatedResponse<PostDTO>>(`/posts/admin?${params}`);
    }

    async createPost(data: CreatePostRequest): Promise<ApiResponse<PostDTO>> {
        return this.request<ApiResponse<PostDTO>>("/posts", {
            method: "POST",
            body: JSON.stringify(data),
        });
    }

    async updatePost(
        id: string,
        data: UpdatePostRequest
    ): Promise<ApiResponse<PostDTO>> {
        return this.request<ApiResponse<PostDTO>>(`/posts/${id}`, {
            method: "PATCH",
            body: JSON.stringify(data),
        });
    }

    async deletePost(id: string): Promise<void> {
        return this.request<void>(`/posts/${id}`, {
            method: "DELETE",
        });
    }

    // ============================================================
    // Categories
    // ============================================================

    async getCategories(): Promise<ApiResponse<CategoryDTO[]>> {
        return this.request<ApiResponse<CategoryDTO[]>>("/categories");
    }

    async createCategory(
        data: CreateCategoryRequest
    ): Promise<ApiResponse<CategoryDTO>> {
        return this.request<ApiResponse<CategoryDTO>>("/categories", {
            method: "POST",
            body: JSON.stringify(data),
        });
    }

    async updateCategory(
        id: string,
        data: CreateCategoryRequest
    ): Promise<ApiResponse<CategoryDTO>> {
        return this.request<ApiResponse<CategoryDTO>>(`/categories/${id}`, {
            method: "PUT",
            body: JSON.stringify(data),
        });
    }

    async deleteCategory(id: string): Promise<void> {
        return this.request<void>(`/categories/${id}`, {
            method: "DELETE",
        });
    }

    // ============================================================
    // Tags
    // ============================================================

    async getTags(): Promise<ApiResponse<TagDTO[]>> {
        return this.request<ApiResponse<TagDTO[]>>("/tags");
    }
}

/** Singleton API client instance */
export const api = new ApiClient();
