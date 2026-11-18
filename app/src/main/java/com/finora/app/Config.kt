package com.finora.app

/** Same Supabase project as the web app (the anon key is a public client key by design). */
object Config {
    const val SUPABASE_URL = "https://xqibpsageodzkisabzbh.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhxaWJwc2FnZW9kemtpc2FiemJoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI1NTMzMTQsImV4cCI6MjA5ODEyOTMxNH0.4fyLHKq-Opf_uqTa4_8nHoofHjry0l52752QCsMlxy8"

    /** Deep link the password-recovery e-mail should open (must be allow-listed in Supabase Auth > URL Configuration). */
    const val RECOVERY_REDIRECT = "finora://auth-callback"
}
