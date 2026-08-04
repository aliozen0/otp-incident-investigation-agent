import type { SVGProps } from 'react'

// Stroke icon set (lucide-style geometry), inlined so the console ships no icon
// dependency and every glyph inherits currentColor + the local stroke weight.
function Icon({ children, size = 16, ...rest }: SVGProps<SVGSVGElement> & { size?: number }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...rest}
    >
      {children}
    </svg>
  )
}

type P = SVGProps<SVGSVGElement> & { size?: number }

export const IconPlus = (p: P) => (
  <Icon {...p}><path d="M12 5v14M5 12h14" /></Icon>
)
export const IconSearch = (p: P) => (
  <Icon {...p}><circle cx="11" cy="11" r="7" /><path d="m20 20-3.2-3.2" /></Icon>
)
export const IconSend = (p: P) => (
  <Icon {...p}><path d="M12 19V5" /><path d="m5 12 7-7 7 7" /></Icon>
)
export const IconSettings = (p: P) => (
  <Icon {...p}>
    <circle cx="12" cy="12" r="3" />
    <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.7 1.7 0 0 0-1.87-.34 1.7 1.7 0 0 0-1 1.56V21a2 2 0 1 1-4 0v-.09A1.7 1.7 0 0 0 8.9 19.3a1.7 1.7 0 0 0-1.87.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.7 1.7 0 0 0 4.7 15a1.7 1.7 0 0 0-1.56-1H3a2 2 0 1 1 0-4h.09A1.7 1.7 0 0 0 4.7 9a1.7 1.7 0 0 0-.34-1.87l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.7 1.7 0 0 0 9 4.7h.08A1.7 1.7 0 0 0 10 3.14V3a2 2 0 1 1 4 0v.09a1.7 1.7 0 0 0 1 1.56 1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.7 1.7 0 0 0 19.3 9v.08a1.7 1.7 0 0 0 1.56 1H21a2 2 0 1 1 0 4h-.09a1.7 1.7 0 0 0-1.56 1Z" />
  </Icon>
)
export const IconClose = (p: P) => (
  <Icon {...p}><path d="M18 6 6 18M6 6l12 12" /></Icon>
)
export const IconClock = (p: P) => (
  <Icon {...p}><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></Icon>
)
export const IconSparkles = (p: P) => (
  <Icon {...p}>
    <path d="M12 3.5 13.6 8 18 9.6 13.6 11.2 12 15.7 10.4 11.2 6 9.6 10.4 8Z" />
    <path d="M18.5 15.5 19.2 17.3 21 18l-1.8.7-.7 1.8-.7-1.8L16 18l1.8-.7Z" />
  </Icon>
)
export const IconMessage = (p: P) => (
  <Icon {...p}><path d="M20 15a2 2 0 0 1-2 2H8l-4 3V6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2Z" /></Icon>
)
export const IconBook = (p: P) => (
  <Icon {...p}>
    <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H19v15H6.5A2.5 2.5 0 0 0 4 20.5Z" />
    <path d="M4 20.5A2.5 2.5 0 0 1 6.5 18H19v3H6.5" />
  </Icon>
)
export const IconSidebarCollapse = (p: P) => (
  <Icon {...p}>
    <rect x="3" y="4" width="18" height="16" rx="2.5" />
    <path d="M10 4v16" />
    <path d="m7 10-2 2 2 2" />
  </Icon>
)
export const IconSidebarExpand = (p: P) => (
  <Icon {...p}>
    <rect x="3" y="4" width="18" height="16" rx="2.5" />
    <path d="M10 4v16" />
    <path d="m5 10 2 2-2 2" />
  </Icon>
)
export const IconTrash = (p: P) => (
  <Icon {...p}>
    <path d="M4 7h16M10 7V5.5A1.5 1.5 0 0 1 11.5 4h1A1.5 1.5 0 0 1 14 5.5V7" />
    <path d="M6 7v12.5A1.5 1.5 0 0 0 7.5 21h9a1.5 1.5 0 0 0 1.5-1.5V7" />
    <path d="M10 11v6M14 11v6" />
  </Icon>
)
export const IconPencil = (p: P) => (
  <Icon {...p}>
    <path d="M4 20h4l10.5-10.5a2.12 2.12 0 0 0-3-3L5 17Z" />
    <path d="m14.5 6.5 3 3" />
  </Icon>
)
export const IconCopy = (p: P) => (
  <Icon {...p}>
    <rect x="9" y="9" width="11" height="11" rx="2" />
    <path d="M5 15a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h7a2 2 0 0 1 2 2" />
  </Icon>
)
export const IconCheck = (p: P) => (
  <Icon {...p}><path d="m5 12.5 4.5 4.5L19 7.5" /></Icon>
)
export const IconArrowDown = (p: P) => (
  <Icon {...p}><path d="M12 5v14" /><path d="m5 12 7 7 7-7" /></Icon>
)
export const IconChevronRight = (p: P) => (
  <Icon {...p}><path d="m9 5 7 7-7 7" /></Icon>
)
export const IconUpload = (p: P) => (
  <Icon {...p}><path d="M12 16V4" /><path d="m7 9 5-5 5 5" /><path d="M4 16v2.5A1.5 1.5 0 0 0 5.5 20h13a1.5 1.5 0 0 0 1.5-1.5V16" /></Icon>
)
export const IconShield = (p: P) => (
  <Icon {...p}><path d="M12 3 5 6v6c0 4.2 2.8 7.6 7 9 4.2-1.4 7-4.8 7-9V6Z" /><path d="m9 12 2 2 4-4" /></Icon>
)
export const IconActivity = (p: P) => (
  <Icon {...p}><path d="M3 12h4l2.5-7 5 14L17 12h4" /></Icon>
)
export const IconAlert = (p: P) => (
  <Icon {...p}><path d="M12 4 2.7 20h18.6Z" /><path d="M12 10v4M12 17.2h.01" /></Icon>
)
export const IconKeyboard = (p: P) => (
  <Icon {...p}>
    <rect x="2.5" y="6" width="19" height="12" rx="2" />
    <path d="M6.5 10h.01M10 10h.01M13.5 10h.01M17 10h.01M8 14h8" />
  </Icon>
)
export const IconLayers = (p: P) => (
  <Icon {...p}><path d="m12 3 9 5-9 5-9-5Z" /><path d="m3 14 9 5 9-5" /></Icon>
)
export const IconDatabase = (p: P) => (
  <Icon {...p}>
    <ellipse cx="12" cy="6" rx="7.5" ry="3" />
    <path d="M4.5 6v6c0 1.66 3.36 3 7.5 3s7.5-1.34 7.5-3V6" />
    <path d="M4.5 12v6c0 1.66 3.36 3 7.5 3s7.5-1.34 7.5-3v-6" />
  </Icon>
)
export const IconChevronDown = (p: P) => (
  <Icon {...p}><path d="m5 9 7 7 7-7" /></Icon>
)
export const IconRefresh = (p: P) => (
  <Icon {...p}><path d="M20 12a8 8 0 1 1-2.6-5.9" /><path d="M20 4v4.5h-4.5" /></Icon>
)
