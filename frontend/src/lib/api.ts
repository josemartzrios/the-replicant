/**
 * API Client for The Replicant backend.
 *
 * Centralized fetch wrapper with:
 * - JWT token injection via HttpOnly Cookies (credentials: 'include')
 * - Automatic 401 handling (clear token, redirect to login)
 * - Typed responses matching backend DTOs
 * - RFC 7807 error parsing
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
    // accessToken is handled automatically by the browser via HttpOnly cookies

    private async request<T>(
        path: string,
        options: RequestInit = {}
    ): Promise<T> {
        const headers: Record<string, string> = {
            "Content-Type": "application/json",
            ...((options.headers as Record<string, string>) || {}),
        };

        const response = await fetch(`${API_URL}${path}`, {
            ...options,
            headers,
            credentials: "include", // Essential for sending HttpOnly cookies cross-origin
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
                localStorage.removeItem("user");
                // Note: The /auth/logout API handles clearing the HttpOnly cookie.
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

    async logout(): Promise<void> {
        return this.request<void>("/auth/logout", {
            method: "POST",
        });
    }

    async refreshToken(): Promise<AuthResponse> {
        return this.request<AuthResponse>("/auth/refresh", {
            method: "POST",
        });
    }

    // ============================================================
    // Posts (Public)
    // ============================================================

    async getPosts(
        page = 0,
        size = 10,
        category?: string,
        tag?: string,
        options?: RequestInit
    ): Promise<PaginatedResponse<PostDTO>> {
        const params = new URLSearchParams({
            page: String(page),
            size: String(size),
        });
        if (category) params.set("category", category);
        if (tag) params.set("tag", tag);

        return this.request<PaginatedResponse<PostDTO>>(`/posts?${params}`, options);
    }

    async getPostBySlug(slug: string, options?: RequestInit): Promise<ApiResponse<PostDTO>> {
        return this.request<ApiResponse<PostDTO>>(`/posts/${encodeURIComponent(slug)}`, options);
    }

    async searchPosts(
        query: string,
        page = 0,
        size = 10,
        options?: RequestInit
    ): Promise<PaginatedResponse<PostDTO>> {
        const params = new URLSearchParams({
            q: query,
            page: String(page),
            size: String(size),
        });
        return this.request<PaginatedResponse<PostDTO>>(`/posts/search?${params}`, options);
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

    async getAdminCategories(): Promise<ApiResponse<CategoryDTO[]>> {
        return this.request<ApiResponse<CategoryDTO[]>>("/categories?includeAll=true");
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
            method: "PATCH",
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
