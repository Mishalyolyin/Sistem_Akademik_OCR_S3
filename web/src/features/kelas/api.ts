import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";

export type StudyClass = {
  id: number;
  name: string;
  displayName: string;
  kerjasama: boolean;
  academicYear: string;
  active: boolean;
};

export type StudyClassInput = {
  name: string;
  kerjasama?: boolean;
  academicYear: string;
  active?: boolean;
};

const KEY = "classes";

export function useClasses() {
  return useQuery({
    queryKey: [KEY],
    queryFn: () => apiFetch<StudyClass[]>("/classes"),
  });
}

export function useCreateClass() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (body: StudyClassInput) =>
      apiFetch<StudyClass>("/classes", { method: "POST", body }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}

export function useUpdateClass() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, ...body }: StudyClassInput & { id: number }) =>
      apiFetch<StudyClass>(`/classes/${id}`, { method: "PUT", body }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}

export function useDeleteClass() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) =>
      apiFetch<void>(`/classes/${id}`, { method: "DELETE" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [KEY] }),
  });
}
