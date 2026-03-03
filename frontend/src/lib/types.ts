/**
 * TypeScript types matching backend DTOs.
 *
 * Mirrors the Java DTOs in com.thereplicant.api.dto.*
 * to ensure type-safe API consumption.
 */

// ============================================================
// Auth
// ============================================================

export interface LoginRequest {
    email: string;
    password: string;
}

export interface SetupRequest {
    email: string;
    name: string;
    password: string;
}

export interface AuthResponse {
    accessToken: string;
    refreshToken: string;
    expiresIn: number;
    user: UserDTO;
}

export interface AuthStatus {
    setupRequired: boolean;
}

export interface UserDTO {
    id: string;
    email: string;
    name: string;
    role: "ADMIN" | "AUTHOR";
}

// ============================================================
// Blog
// ============================================================

export type PostStatus = "DRAFT" | "PUBLISHED";

export interface PostDTO {
    id: string;
    title: string;
    slug: string;
    content: string;
    excerpt: string;
    status: PostStatus;
    category: CategoryDTO | null;
    tags: TagDTO[];
    author: AuthorDTO;
    readingTimeMinutes: number;
    publishedAt: string | null;
    createdAt: string;
    updatedAt: string;
}

export interface AuthorDTO {
    id: string;
    name: string;
}

export interface CategoryDTO {
    id: string;
    name: string;
    slug: string;
    description: string | null;
    postCount: number;
}

export interface TagDTO {
    id: string;
    name: string;
    slug: string;
    postCount: number;
}

export interface CreatePostRequest {
    title: string;
    content: string;
    excerpt?: string;
    categoryId?: string;
    tags?: string[];
    status?: string;
}

export interface UpdatePostRequest {
    title?: string;
    content?: string;
    excerpt?: string;
    categoryId?: string;
    tags?: string[];
    status?: string;
}

export interface CreateCategoryRequest {
    name: string;
    description?: string;
}

// ============================================================
// API Responses
// ============================================================

export interface ApiResponse<T> {
    data: T;
    meta: ResponseMeta;
}

export interface PaginatedResponse<T> {
    data: T[];
    meta: PaginationMeta;
}

export interface ResponseMeta {
    timestamp: string;
}

export interface PaginationMeta {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    hasNext: boolean;
    hasPrevious: boolean;
    timestamp: string;
}

/** RFC 7807 Problem Detail */
export interface ApiError {
    type: string;
    title: string;
    status: number;
    detail: string;
    instance?: string;
    errors?: FieldError[];
}

export interface FieldError {
    field: string;
    message: string;
}
