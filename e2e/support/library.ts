import type { APIRequestContext } from '@playwright/test';
import type { BookInput } from './builders';

/** A book as the API returns it (snake_case). */
export interface ApiBook {
  id: string;
  title: string;
  author: string;
  status: string;
  rating: number | null;
  date_completed: string | null;
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

  /** Reads one book by id, or null when the API answers 404. */
  async find(id: string): Promise<ApiBook | null> {
    const response = await this.request.get(`/api/v1/books/${encodeURIComponent(id)}`);
    if (response.status() === 404) return null;
    if (response.status() !== 200) throw new Error(`get failed: ${response.status()}`);
    return response.json();
  }

  /** Creates a book and returns it. */
  async create(input: BookInput): Promise<ApiBook> {
    const response = await this.request.post('/api/v1/books', { headers: JSON_HEADERS, data: input });
    if (response.status() !== 201) throw new Error(`create failed: ${response.status()}`);
    return response.json();
  }

  /** Changes the status of a book (PUT .../status) and returns the updated book. */
  async setStatus(id: string, status: string): Promise<ApiBook> {
    const response = await this.request.put(`/api/v1/books/${encodeURIComponent(id)}/status`,
      { headers: JSON_HEADERS, data: { status } });
    if (response.status() !== 200) throw new Error(`status change failed: ${response.status()}`);
    return response.json();
  }

  /** Rates a completed book (PUT .../rating) and returns the updated book. */
  async rate(id: string, rating: number): Promise<ApiBook> {
    const response = await this.request.put(`/api/v1/books/${encodeURIComponent(id)}/rating`,
      { headers: JSON_HEADERS, data: { rating } });
    if (response.status() !== 200) throw new Error(`rating failed: ${response.status()}`);
    return response.json();
  }

  /**
   * Creates `count` books of maximum size (title 500 and author 200 code points), each unique.
   * @param count how many books to create
   * @param from first sequence number (keeps titles unique across calls)
   */
  async createMaxSize(count: number, from = 0): Promise<void> {
    for (let i = from; i < from + count; i += 1) {
      const tag = `${String(i).padStart(5, '0')} `;
      await this.create({ title: tag + 'T'.repeat(500 - tag.length), author: 'A'.repeat(200) });
    }
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
