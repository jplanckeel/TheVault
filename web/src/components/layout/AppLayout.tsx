import { Suspense } from 'react'
import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import { MobileTabBar, MobileTopBar } from './MobileNav'

/** Application shell: sidebar on desktop, top bar + floating tab bar on small screens. */
export default function AppLayout() {
  return (
    <div className="min-h-screen bg-shell">
      <Sidebar />
      <div className="md:ml-64 min-h-screen flex flex-col">
        <MobileTopBar />
        <main className="flex-1 w-full max-w-[1200px] mx-auto px-4 pt-2 pb-32 md:px-10 md:py-10">
          <Suspense fallback={<div className="min-h-[50vh]" aria-busy="true" />}>
            <Outlet />
          </Suspense>
        </main>
      </div>
      <MobileTabBar />
    </div>
  )
}
