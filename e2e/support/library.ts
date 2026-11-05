import type { APIRequestContext } from '@playwright/test';
import type { BookInput } from './builders';

/** A book as the API returns it (snake_case). */
export interface ApiBook {
  id: string;
  title: string;
  author: string;
  status: string;
  rating: number | null;
}

const JSON_HEADERS = { 'Content-Type': 'application/json' };

/**
 * Seeds and resets the library through the public API only (no test-only endpoint).
 * Every non-GET request carries Content-Type: application/json, DELETE included.
 */
export class Library {
  constructor(private readonly request: APIRequestContext) {}

  /** Lists all books in API order. */
  async list(): Promise<{ books: ApiBook[]; total: number }> {
    const response = await this.request.get('/api/v1/books');
    if (response.status() !== 200) throw new Error(`list failed: ${response.status()}`);
    return response.json();
  }

  /** Creates a book and returns it. */
  async create(input: BookInput): Promise<ApiBook> {
    const response = await this.request.post('/api/v1/books', { headers: JSON_HEADERS, data: input });
    if (response.status() !== 201) throw new Error(`create failed: ${response.status()}`);
    return response.json();
  }

  /** Deletes one book by id. */
  async remove(id: string): Promise<void> {
    const response = await this.request.delete(`/api/v1/books/${encodeURIComponent(id)}`, { headers: JSON_HEADERS });
    if (response.status() !== 200) throw new Error(`delete failed: ${response.status()}`);
  }

  /** Empties the library: lists, then deletes every book. */
  async reset(): Promise<void> {
    const { books } = await this.list();
    for (const book of books) await this.remove(book.id);
  }
}
