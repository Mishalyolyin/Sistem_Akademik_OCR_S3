import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { API_BASE_URL, apiFetch, getAccessToken } from "@/lib/api";

export type RowError = {
  row: number;
  nim: string | null;
  message: string;
};

export type ImportResult = {
  batchId: number;
  filename: string;
  totalRows: number;
  successRows: number;
  failedRows: number;
  errors: RowError[];
};

const KEY = "import-history";

export function useImportHistory() {
  return useQuery({
    queryKey: [KEY],
    queryFn: () => apiFetch<ImportResult[]>("/students/import/history"),
  });
}

export function useImportStudents() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (file: File) => {
      const body = new FormData();
      body.append("file", file);
      return apiFetch<ImportResult>("/students/import", {
        method: "POST",
        body,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [KEY] });
      queryClient.invalidateQueries({ queryKey: ["students"] });
      queryClient.invalidateQueries({ queryKey: ["classes"] });
    },
  });
}

/**
 * Unduh template .xlsx. Tidak bisa memakai <a href> biasa karena endpoint-nya
 * butuh header Authorization, jadi berkasnya diambil dulu sebagai blob.
 */
export async function downloadTemplate() {
  const base = API_BASE_URL.endsWith("/") ? API_BASE_URL : `${API_BASE_URL}/`;
  const response = await fetch(new URL("students/import/template", base), {
    headers: { Authorization: `Bearer ${getAccessToken()}` },
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error("Gagal mengunduh template.");
  }

  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "template-import-mahasiswa.xlsx";
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
