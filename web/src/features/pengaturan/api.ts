import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { API_BASE_URL, apiFetch, getAccessToken } from "@/lib/api";

export type SystemSetting = {
  key: string;
  value: string | null;
  description: string | null;
  updatedAt: string;
};

const KEY = "settings";

export function useSettings() {
  return useQuery({
    queryKey: [KEY],
    queryFn: () => apiFetch<SystemSetting[]>("/settings"),
  });
}

export function useUpdateSetting() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) =>
      apiFetch<SystemSetting>(`/settings/${key}`, {
        method: "PUT",
        body: { value },
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}

/** Unduhan butuh header Authorization, jadi diambil sebagai blob dulu. */
export async function unduhBerkas(path: string, namaBerkas: string) {
  const base = API_BASE_URL.endsWith("/") ? API_BASE_URL : `${API_BASE_URL}/`;
  const response = await fetch(new URL(path, base), {
    headers: { Authorization: `Bearer ${getAccessToken()}` },
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error("Berkas gagal diunduh.");
  }

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = namaBerkas;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
