import { describe, it, expect } from 'vitest'
import { generateIdempotencyKey } from './idempotency'

describe('generateIdempotencyKey', () => {
  it('returns a valid UUID string', () => {
    const key = generateIdempotencyKey()
    expect(key).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i)
  })

  it('returns a different value on each call', () => {
    expect(generateIdempotencyKey()).not.toBe(generateIdempotencyKey())
  })
})
