import { http } from "@/shared/services/http";
import type { Paginated, PageQuery } from "@/shared/types/api";

/** User row as returned by the backend list endpoint (joined DTO). */
export interface UserRow {
  publicId: string;
  username: string;
  fullName: string;
  email: string;
  active: boolean;
  roles: string[];
  createdAt: string;
}

/** Axios calls for the access/users feature (ARCHITECTURE.md §3 module api/). */
export const usersApi = {
  list(query: PageQuery): Promise<Paginated<UserRow>> {
    return http
      .get<Paginated<UserRow>>("/access/users", { params: query })
      .then((r) => r.data);
  },
};
