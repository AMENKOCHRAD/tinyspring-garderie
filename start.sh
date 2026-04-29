#!/bin/bash
# ============================================================
#  TinySpring Garderie — Lanceur complet
#  Backend (8081) + Front-office (4200) + Back-office (4201)
# ============================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend/garderie"
FRONT_OFFICE_DIR="$PROJECT_ROOT/frontend/front-office"
BACK_OFFICE_DIR="$PROJECT_ROOT/frontend/back-office"
PYTHON_AI_DIR="$PROJECT_ROOT/python-ai"

BACKEND_PID=""
FRONT_PID=""
BACK_PID=""
PYTHON_AI_PID=""

# ── Cleanup on exit ──────────────────────────────────────────
cleanup() {
    echo ""
    echo -e "${YELLOW}⏹  Arrêt de tous les services...${NC}"
    
    [ -n "$BACKEND_PID" ] && kill "$BACKEND_PID" 2>/dev/null && echo -e "   ${RED}✗${NC} Backend arrêté"
    [ -n "$FRONT_PID" ]   && kill "$FRONT_PID"   2>/dev/null && echo -e "   ${RED}✗${NC} Front-office arrêté"
    [ -n "$BACK_PID" ]    && kill "$BACK_PID"     2>/dev/null && echo -e "   ${RED}✗${NC} Back-office arrêté"
    [ -n "$PYTHON_AI_PID" ] && kill "$PYTHON_AI_PID" 2>/dev/null && echo -e "   ${RED}✗${NC} Python ML arrêté"
    
    # Kill child processes too
    [ -n "$BACKEND_PID" ] && pkill -P "$BACKEND_PID" 2>/dev/null
    [ -n "$FRONT_PID" ]   && pkill -P "$FRONT_PID"   2>/dev/null
    [ -n "$BACK_PID" ]    && pkill -P "$BACK_PID"     2>/dev/null
    [ -n "$PYTHON_AI_PID" ] && pkill -P "$PYTHON_AI_PID" 2>/dev/null
    
    echo -e "${GREEN}✔  Tout est arrêté proprement.${NC}"
    exit 0
}
trap cleanup SIGINT SIGTERM EXIT

# ── Header ───────────────────────────────────────────────────
echo -e "${CYAN}${BOLD}"
echo "  ╔══════════════════════════════════════════════╗"
echo "  ║       🏫 TinySpring Garderie — Launcher      ║"
echo "  ╚══════════════════════════════════════════════╝"
echo -e "${NC}"

# ── Check directories ────────────────────────────────────────
for dir in "$BACKEND_DIR" "$FRONT_OFFICE_DIR" "$BACK_OFFICE_DIR" "$PYTHON_AI_DIR"; do
    if [ ! -d "$dir" ]; then
        echo -e "${RED}✗ Dossier manquant : $dir${NC}"
        exit 1
    fi
done

# ── Check node_modules ───────────────────────────────────────
for dir in "$FRONT_OFFICE_DIR" "$BACK_OFFICE_DIR"; do
    if [ ! -d "$dir/node_modules" ]; then
        echo -e "${YELLOW}⚠  node_modules manquant dans $(basename "$(dirname "$dir")")/$(basename "$dir"), installation...${NC}"
        (cd "$dir" && npm install)
    fi
done

# ── 1. Backend (Spring Boot — port 8081) ─────────────────────
echo -e "${CYAN}[1/4]${NC} ${BOLD}Backend${NC} Spring Boot sur ${GREEN}http://localhost:8081${NC}"
(cd "$BACKEND_DIR" && ./mvnw spring-boot:run -q 2>&1 | sed 's/^/       [backend] /') &
BACKEND_PID=$!

# Wait a bit for backend to start initializing
sleep 3

# ── 2. Front-office (Angular — port 4200) ────────────────────
echo -e "${CYAN}[2/4]${NC} ${BOLD}Front-office${NC} Angular sur ${GREEN}http://localhost:4200${NC}"
(cd "$FRONT_OFFICE_DIR" && npx ng serve --port 4200 2>&1 | sed 's/^/       [front]   /') &
FRONT_PID=$!

# ── 3. Back-office (Angular — port 4201) ─────────────────────
echo -e "${CYAN}[3/4]${NC} ${BOLD}Back-office${NC} Angular sur ${GREEN}http://localhost:4201${NC}"
(cd "$BACK_OFFICE_DIR" && npx ng serve --port 4201 2>&1 | sed 's/^/       [back]    /') &
BACK_PID=$!

# ── 4. Python ML (FastAPI — port 8000) ───────────────────────
echo -e "${CYAN}[4/4]${NC} ${BOLD}Python ML${NC} FastAPI sur ${GREEN}http://localhost:8000${NC}"
(cd "$PYTHON_AI_DIR" && source venv/bin/activate && python local_ai_service.py 2>&1 | sed 's/^/       [ai-ml]   /') &
PYTHON_AI_PID=$!

# ── Status ───────────────────────────────────────────────────
echo ""
echo -e "${GREEN}${BOLD}  ✔ Les 4 services démarrent...${NC}"
echo ""
echo -e "  ┌─────────────────────────────────────────────────────────┐"
echo -e "  │  ${BOLD}Service${NC}          │  ${BOLD}URL${NC}                       │  ${BOLD}Rôle${NC}       │"
echo -e "  ├─────────────────────────────────────────────────────────┤"
echo -e "  │  Backend         │  http://localhost:${GREEN}8081${NC}     │  API        │"
echo -e "  │  Front-office    │  http://localhost:${GREEN}4200${NC}     │  Parents    │"
echo -e "  │  Back-office     │  http://localhost:${GREEN}4201${NC}     │  Admin      │"
echo -e "  │  Python ML       │  http://localhost:${GREEN}8000${NC}     │  IA Reco    │"
echo -e "  └─────────────────────────────────────────────────────────┘"
echo ""
echo -e "  ${BOLD}Comptes de test :${NC}"
echo -e "    Admin       →  admin@garderie.com      / admin123   (back-office)"
echo -e "    Parent      →  parent@garderie.com     / parent123  (front-office)"
echo -e "    Animatrice  →  animatrice@garderie.com / anim123    (front-office)"
echo ""
echo -e "  ${YELLOW}Appuyez sur Ctrl+C pour tout arrêter${NC}"
echo ""

# ── Wait for all processes ───────────────────────────────────
wait
