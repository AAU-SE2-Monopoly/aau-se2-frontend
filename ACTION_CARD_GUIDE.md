/**
 * ACTION CARD SYSTEM IMPLEMENTATION GUIDE
 * 
 * This system displays action cards (Chance, Community Chest) with an "Execute Action" button
 * in the center of the screen. The player must press the button before the game can continue.
 * 
 * ============ ARCHITECTURE ============
 * 
 * 1. ActionCardOverlay.kt
 *    - Composable UI for displaying action cards
 *    - Shows card details (money, jail, movement, etc.)
 *    - Displays Execute Action button
 *    - Manages button states (enabled/disabled)
 * 
 * 2. GameViewModel Updates
 *    - currentActionCard: StateFlow<Card?>
 *    - isExecutingAction: StateFlow<Boolean>
 *    - showActionCardOverlay: StateFlow<Boolean>
 *    - executeAction(): Function to send action to backend
 *    - setCurrentActionCard(card: Card?): Set card to display
 *    - dismissActionCard(): Clear the action card
 * 
 * 3. GameService Update
 *    - executeAction(playerId: String): Override function
 *    - Sends EXECUTE_ACTION to backend
 * 
 * 4. GameStompClient Implementation
 *    - executeAction() sends action via STOMP to /app/game/action
 * 
 * ============ ACTION TYPES SUPPORTED ============
 * 
 * CardAction.COLLECT_MONEY         → Show "+$XX" with green text
 * CardAction.PAY_MONEY             → Show "-$XX" with red text
 * CardAction.COLLECT_FROM_EACH      → Show "+$XX each" from all players
 * CardAction.PAY_EACH_PLAYER        → Show "-$XX each" to all players
 * CardAction.MOVE_TO               → Show "Advance to Field #XX"
 * CardAction.MOVE_FORWARD          → Show "Move XX spaces"
 * CardAction.GO_TO_JAIL            → Show " Go to Jail"
 * CardAction.GET_OUT_OF_JAIL       → Show " Get Out of Jail Free"
 * 
 * ============ INTEGRATION INTO GAMEBOARDUI ============
 * 
 * Step 1: Add ActionCardOverlay to GameboardUI
 * 
 *     @Composable
 *     fun GameboardScreen(modifier: Modifier = Modifier, viewModel: GameViewModel) {
 *         val fields by viewModel.fields.collectAsState(initial = emptyList())
 *         val gameState by viewModel.gameState.collectAsState()
 *         val players = gameState?.players ?: emptyList()
 * 
 *         // Add these new states:
 *         val currentActionCard by viewModel.currentActionCard.collectAsState()
 *         val showActionOverlay by viewModel.showActionCardOverlay.collectAsState()
 *         val isExecutingAction by viewModel.isExecutingAction.collectAsState()
 * 
 *         LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
 * 
 *         Box(modifier = Modifier.fillMaxSize()) {
 *             GameboardContent(fields ?: emptyList(), players, modifier)
 * 
 *             // Add ActionCardOverlay
 *             ActionCardOverlay(
 *                 isVisible = showActionOverlay,
 *                 card = currentActionCard,
 *                 isExecuting = isExecutingAction,
 *                 onExecuteAction = { viewModel.executeAction() },
 *                 onDismiss = { viewModel.dismissActionCard() }
 *             )
 *         }
 *     }
 * 
 * Step 2: Backend Integration
 * 
 * The backend should:
 *   a) Send an event when a card is drawn (e.g., "ACTION_DRAWN")
 *      Event payload: { "card": { "id": 1, "description": "...", "action": "COLLECT_MONEY", ... } }
 *   
 *   b) Subscribe to the card event in GameViewModel:
 *      val actionCardDrawnEvent = gameEventFlow
 *          .filter { it.event == "ACTION_DRAWN" }
 *          .collect { event ->
 *              val card = parseCard(event.payload)
 *              setCurrentActionCard(card)
 *          }
 *   
 *   c) Handle EXECUTE_ACTION action:
 *      - Execute the card's action (transfer money, move player, etc.)
 *      - Send updated GameState back to all players
 *      - Clear the action card from UI
 * 
 * ============ BACKEND PROTOCOL ============
 * 
 * Incoming (Frontend receives):
 * {
 *   "event": "ACTION_DRAWN",
 *   "gameId": "xxx",
 *   "card": {
 *     "id": 1,
 *     "description": "Advance to Go",
 *     "action": "COLLECT_MONEY",
 *     "amount": 200
 *   }
 * }
 * 
 * Outgoing (Frontend sends):
 * {
 *   "gameId": "xxx",
 *   "playerId": "yyy",
 *   "action": "EXECUTE_ACTION",
 *   "payload": {}
 * }
 * 
 * ============ TESTING ============
 * 
 * Test file: ActionCardOverlayTest.kt
 * Coverage: 20+ test cases
 * 
 * Run tests:
 *   ./gradlew testDebugUnitTest -Dorg.gradle.workers.max=4
 * 
 * Test categories:
 *   - Visibility tests (shown/hidden)
 *   - Card type display (Chance vs Community Chest)
 *   - Action detail display (all 8 action types)
 *   - Button state tests (enabled/disabled)
 *   - Button click tests
 *   - Description and instruction text
 * 
 * ============ STYLING ============
 * 
 * Colors:
 *   - Chance Card Header: Color(0xFFF57C00) [Orange]
 *   - Community Chest Header: Color(0xFF1565C0) [Blue]
 *   - Execute Button: Color(0xFF1B7F1C) [Green]
 *   - Disabled Button: Color.Gray
 *   - Money Positive: Color(0xFF2E7D32) [Green]
 *   - Money Negative: Color(0xFFC62828) [Red]
 * 
 * Dimensions:
 *   - Card Width: 400.dp
 *   - Card Height: 550.dp
 *   - Button Height: 50.dp
 *   - Overlay Background: Black (70% opacity)
 * 
 * ============ STATE FLOW DIAGRAM ============
 * 
 *     Backend sends "ACTION_DRAWN" event
 *              ↓
 *     GameViewModel.currentActionCard = Card
 *              ↓
 *     showActionCardOverlay = true
 *              ↓
 *     ActionCardOverlay displays
 *              ↓
 *     Player clicks "Execute Action"
 *              ↓
 *     isExecutingAction = true
 *     executeAction() sends to backend
 *              ↓
 *     Backend processes action
 *     Sends GameState update
 *              ↓
 *     Currently action clears after 500ms
 *     isExecutingAction = false
 *     currentActionCard = null
 *     showActionCardOverlay = false
 * 
 * ============ CHANGELOG ============
 * 
 * v1.0 (2026-05-06)
 *   - Initial ActionCardOverlay implementation
 *   - Support for all 8 CardAction types
 *   - GameViewModel integration
 *   - GameService/GameStompClient updates
 *   - 20+ comprehensive tests
 *   - Documentation with integration guide
 */
