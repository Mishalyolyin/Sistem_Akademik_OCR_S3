import { redirect } from "next/navigation";

export default function Home() {
  // Pengarahan sesuai peran dilakukan halaman login setelah sesi diketahui.
  redirect("/login");
}
