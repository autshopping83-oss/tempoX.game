/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { createContext, useCallback, useContext, useMemo, useState } from "react";

/** User-selectable language — mirrors native LangMode. */
export type LangMode = "SYSTEM" | "PT" | "EN";
type Lang = "PT" | "EN";

const STORAGE_KEY = "60s_lang";

function detectSystemLang(): Lang {
  if (typeof navigator !== "undefined" && navigator.language) {
    return navigator.language.toLowerCase().startsWith("pt") ? "PT" : "EN";
  }
  return "PT";
}

function resolveLang(mode: LangMode): Lang {
  return mode === "SYSTEM" ? detectSystemLang() : mode;
}

function loadMode(): LangMode {
  if (typeof window === "undefined") return "SYSTEM";
  const saved = localStorage.getItem(STORAGE_KEY);
  return saved === "PT" || saved === "EN" ? saved : "SYSTEM";
}

/** PT / EN dictionary — keys are stable ids shared by all screens. */
const DICT = {
  // Navigation
  nav_home: ["Início", "Home"],
  nav_rules: ["Regras", "Rules"],
  nav_start: ["START", "START"],

  // Rules sheet
  rules_title: ["COMO JOGAR", "HOW TO PLAY"],
  rules_subtitle: ["REGRAS DO TEMPOX", "TEMPOX RULES"],
  rules_intro: [
    "é um jogo rápido de agilidade mental extrema. Você tem exatamente um minuto para resolver uma sequência sem fim de testes rápidos:",
    "is a fast game of extreme mental agility. You have exactly one minute to solve an endless stream of quick tests:",
  ],
  rules_memory_desc: ["Decore e repita a sequência exibida de formas coloridas.", "Memorize and repeat the sequence of colored shapes."],
  rules_reflex_desc: ["Toque rápido no alvo dourado. Nunca toque nos de perigo vermelho!", "Tap the golden target fast. Never touch the red danger ones!"],
  rules_math_desc: ["Faça cálculos de múltipla escolha sob pressão extrema.", "Solve multiple-choice equations under extreme pressure."],
  rules_attention_desc: ["Localize instantaneamente o símbolo que é diferente dos outros.", "Instantly find the symbol that differs from the others."],
  rules_combo_tip: [
    "🔥 Acertos consecutivos geram COMBOS de pontos e aceleram os desafios! Erros quebram o combo.",
    "🔥 Consecutive hits build score COMBOS and speed challenges up! Mistakes break the combo.",
  ],
  rules_cta: ["ENTENDI, VAMOS LÁ", "GOT IT, LET'S GO"],

  // Settings
  settings_sound: ["Efeitos de Som", "Sound Effects"],
  settings_vibration: ["Vibração Háptica", "Haptic Vibration"],
  settings_volume: ["Volume dos Efeitos", "Effects Volume"],
  settings_language: ["Idioma", "Language"],
  lang_auto: ["Auto", "Auto"],

  // Home
  home_premium_badge: ["ARCADE PREMIUM", "PREMIUM ARCADE"],
  home_title: ["TEMPOX", "TEMPOX"],
  home_tagline: ["PENSE RÁPIDO. REAJA MAIS RÁPIDO.", "THINK FAST. REACT FASTER."],
  home_level_profile: ["Nível de Perfil", "Profile Level"],
  home_level: ["Nível", "Level"],
  home_best_score: ["★ MELHOR PONTUAÇÃO", "★ BEST SCORE"],
  home_xp_accumulated: ["XP acumulados", "XP accumulated"],
  home_percent_to_level: ["% para Nível", "% to Level"],
  home_play_now: ["JOGAR AGORA", "PLAY NOW"],
  home_seed_label: ["Seed da Partida", "Match Seed"],
  home_seed_placeholder: ["Opcional", "Optional"],
  card_pattern_header: ["DESAFIO MEMÓRIA", "MEMORY CHALLENGE"],
  card_pattern_caption: ["Sequências que aceleram", "Sequences that speed up"],
  card_calc_header: ["DESAFIO CÁLCULO", "MATH CHALLENGE"],
  card_calc_caption: ["Contas sob pressão", "Math under pressure"],
  tab_play: ["Jogar", "Play"],
  tab_stats: ["Estatísticas", "Stats"],
  tab_trophies: ["Troféus", "Trophies"],
  stats_historical_title: ["Estatísticas Históricas", "Lifetime Statistics"],
  stats_matches: ["Partidas", "Matches"],
  stats_record: ["Recorde", "Record"],
  stats_total_xp: ["Total XP", "Total XP"],
  stats_max_combo: ["Combo Máximo", "Max Combo"],
  stats_answers: ["Respostas", "Answers"],
  stats_avg_accuracy: ["Precisão Média", "Avg Accuracy"],
  trophies_title: ["Troféus e Conquistas", "Trophies & Achievements"],

  // Challenges
  hud_points: ["Pontos", "Points"],
  hud_combo_chip: ["🔥 COMBO x%1", "🔥 COMBO x%1"],
  challenge_memory_name: ["Memória", "Memory"],
  challenge_reflex_name: ["Reflexo", "Reflex"],
  challenge_math_name: ["Matemática", "Math"],
  challenge_attention_name: ["Atenção", "Attention"],
  challenge_memory_instruction: ["Decore e repita a sequência", "Memorize and repeat the sequence"],
  challenge_reflex_instruction: ["Toque rápido no alvo amarelo", "Tap the yellow target fast"],
  challenge_math_instruction: ["Resolva a equação o mais rápido", "Solve the equation as fast as you can"],
  challenge_attention_instruction: ["Toque no símbolo diferente", "Tap the odd symbol"],
  memory_watch_phase: ["👀 OBSERVE A SEQUÊNCIA", "👀 WATCH THE SEQUENCE"],
  memory_input_phase: ["✋ SUA VEZ!", "✋ YOUR TURN!"],

  // Pause
  pause_badge: ["JOGO EM PAUSA", "GAME PAUSED"],
  pause_heading: ["PAUSADO", "PAUSED"],
  pause_subtitle: ["Respire fundo. Retorne quando estiver pronto!", "Take a breath. Come back when you're ready!"],
  pause_resume: ["RETOMAR PARTIDA", "RESUME MATCH"],
  pause_restart: ["RECOMEÇAR DO ZERO", "RESTART FROM ZERO"],
  pause_quit: ["Abandonar Partida", "Abandon Match"],

  // Result
  result_time_up: ["TEMPO ESGOTADO!", "TIME'S UP!"],
  result_final_score: ["Pontuação Final", "Final Score"],
  result_new_record: ["🏆 NOVO RECORDE REGISTRADO!", "🏆 NEW RECORD SET!"],
  stat_challenges: ["Desafios", "Challenges"],
  stat_accuracy: ["Precisão", "Accuracy"],
  stat_max_combo: ["Combo Máx", "Max Combo"],
  result_reward_pool: ["RECOMPENSA ACUMULADA", "ACCUMULATED REWARD"],
  result_double_hint: ["Dobre seus ganhos assistindo a um vídeo curto!", "Double your earnings by watching a short video!"],
  result_double_btn: ["🎬 DOBRAR RECOMPENSA", "🎬 DOUBLE REWARD"],
  result_doubled_ok: ["✓ DOBRADO COM SUCESSO!", "✓ DOUBLED SUCCESSFULLY!"],
  result_ad_loading: ["Carregando Anúncio...", "Loading Ad..."],
  result_ad_video: ["Vídeo Premiado", "Rewarded Video"],
  result_ad_note: ["Ganhe o dobro de XP ao fim do anúncio promocional.", "Earn double XP at the end of the promo ad."],
  result_new_trophies: ["NOVOS TROFÉUS CONQUISTADOS!", "NEW TROPHIES UNLOCKED!"],
  result_play_again: ["JOGAR NOVAMENTE", "PLAY AGAIN"],
  action_share: ["COMPARTILHAR", "SHARE"],
  action_home: ["MENU PRINCIPAL", "MAIN MENU"],
  share_text: [
    "Fiz %1 pontos com combo x%2 no TEMPOX! Consegue superar?",
    "I scored %1 points with a x%2 combo in TEMPOX! Can you beat it?",
  ],
  share_alert: ["Pontuação compartilhada: %1 pontos!", "Shared score: %1 points!"],

  // Achievements (title / description per id)
  ach_elephant_title: ["🧠 Memória de Elefante", "🧠 Elephant Memory"],
  ach_elephant_desc: ["Completar sequência de 7 ou mais símbolos na Memória.", "Complete a sequence of 7+ symbols in Memory."],
  ach_reflex_title: ["⚡ Reflexo Perfeito", "⚡ Perfect Reflex"],
  ach_reflex_desc: ["Responder um desafio de reflexo em menos de 0.25 segundos.", "Answer a reflex challenge in under 0.25 seconds."],
  ach_unstoppable_title: ["🔥 Imparável", "🔥 Unstoppable"],
  ach_unstoppable_desc: ["Alcançar Combo x10 durante uma partida.", "Reach Combo x10 during a match."],
  ach_survivor_title: ["⏱️ Sobrevivente", "⏱️ Survivor"],
  ach_survivor_desc: ["Completar uma partida inteira de 60 segundos.", "Complete an entire 60-second match."],
  ach_recordist_title: ["🏆 Recordista", "🏆 Record Breaker"],
  ach_recordist_desc: ["Superar o seu próprio recorde histórico de pontuação.", "Beat your own all-time high score."],

  // Trophies screen
  trophy_unlocked: ["✓ LIBERADO", "✓ UNLOCKED"],
  trophy_locked: ["BLOQUEADO", "LOCKED"],
} as const;

export type DictKey = keyof typeof DICT;

interface I18nContextValue {
  mode: LangMode;
  lang: Lang;
  setMode: (mode: LangMode) => void;
  t: (key: DictKey, args?: Array<string | number>) => string;
}

const I18nContext = createContext<I18nContextValue | null>(null);

export function LangProvider({ children }: { children: React.ReactNode }) {
  const [mode, setModeState] = useState<LangMode>(loadMode);
  const lang = useMemo(() => resolveLang(mode), [mode]);

  const setMode = useCallback((next: LangMode) => {
    setModeState(next);
    try {
      if (next === "SYSTEM") localStorage.removeItem(STORAGE_KEY);
      else localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // storage unavailable — session-only language
    }
  }, []);

  const t = useCallback(
    (key: DictKey, args?: Array<string | number>) => {
      const entry: readonly [string, string] = DICT[key];
      let out: string = lang === "EN" ? entry[1] : entry[0];
      if (args) {
        args.forEach((v, i) => {
          out = out.replace(`%${i + 1}`, String(v));
        });
      }
      return out;
    },
    [lang]
  );

  const value = useMemo(() => ({ mode, lang, setMode, t }), [mode, lang, setMode, t]);
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nContextValue {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error("useI18n must be used inside <LangProvider>");
  return ctx;
}
