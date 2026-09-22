package de.omegazirkel.risingworld.playerfactions.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.omegazirkel.risingworld.playerfactions.Faction;
import de.omegazirkel.risingworld.playerfactions.FactionApplication;
import de.omegazirkel.risingworld.playerfactions.FactionApplicationStatus;
import de.omegazirkel.risingworld.playerfactions.FactionMember;
import de.omegazirkel.risingworld.playerfactions.FactionRole;
import de.omegazirkel.risingworld.playerfactions.FactionService;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.PlayerDatabaseHelper.PlayerRecord;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedBaseButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonState;
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlayWithTabs;
import de.omegazirkel.risingworld.tools.ui.Dropdown;
import de.omegazirkel.risingworld.tools.ui.DropdownOption;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.MessageBoxButtons;
import net.risingworld.api.ui.UITextField;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.Pivot;
import net.risingworld.api.ui.style.TextAnchor;

/** Player-facing faction workflow. Tab visibility is based on faction membership, never admin status. */
public final class FactionOverlay extends BasePluginOverlayWithTabs {
    private enum Tab { LIST, CREATE, APPLICATIONS, APPLICANTS, MEMBERS, MINE }
    private static final float TABLE_HEIGHT = 330f;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private final FactionService factions;
    private final String pluginName;
    private Tab tab = Tab.LIST;

    public FactionOverlay(Player player, String pluginName, FactionService factions) {
        super(player, p -> p.deleteAttribute("player-factions.overlay"));
        this.pluginName = pluginName;
        this.factions = factions;
        rebuild();
    }

    @Override protected I18n t() { return I18n.getInstance(pluginName); }
    @Override protected String titleText() { return text("tc.factions.ui.title"); }
    @Override protected String descriptionText() { return text("tc.factions.ui.desc"); }
    @Override protected String legendText() {
        if (tab == Tab.MEMBERS && factions.isLeader(uiPlayer.getDbID())) return text("tc.factions.members.legend.leader");
        if (tab == Tab.MEMBERS && factions.canManageMembers(uiPlayer.getDbID())) return text("tc.factions.members.legend.officer");
        return text("tc.factions.ui.legend");
    }

    @Override protected void setupTabs() {
        boolean member = factions.factionIdFor(uiPlayer.getDbID()) != null;
        setupTabContainer();
        if (member) {
            addTab(text("tc.factions.ui.tab.members"), 135, tab == Tab.MEMBERS, () -> select(Tab.MEMBERS));
            if (factions.canManageApplications(uiPlayer.getDbID())) addTab(text("tc.factions.applicants.tab"), 165, tab == Tab.APPLICANTS, () -> select(Tab.APPLICANTS));
            addTab(text("tc.factions.ui.tab.mine"), 145, tab == Tab.MINE, () -> select(Tab.MINE));
            addTab(text("tc.factions.ui.tab.list"), 135, tab == Tab.LIST, () -> select(Tab.LIST));
            if (tab == Tab.CREATE || tab == Tab.APPLICATIONS || (tab == Tab.APPLICANTS && !factions.canManageApplications(uiPlayer.getDbID()))) tab = Tab.MEMBERS;
        } else {
            addTab(text("tc.factions.ui.tab.list"), 135, tab == Tab.LIST, () -> select(Tab.LIST));
            addTab(text("tc.factions.ui.tab.create"), 160, tab == Tab.CREATE, () -> select(Tab.CREATE));
            addTab(text("tc.factions.ui.tab.applications"), 145, tab == Tab.APPLICATIONS, () -> select(Tab.APPLICATIONS));
            if (tab == Tab.MEMBERS || tab == Tab.MINE || tab == Tab.APPLICANTS) tab = Tab.LIST;
        }
        render();
    }

    private void select(Tab value) { tab = value; rebuild(); }
    private void render() { body.removeAllChilds(); switch (tab) { case LIST -> factionTable(); case CREATE -> create(); case APPLICATIONS -> applicationsTable(); case APPLICANTS -> applicantsTable(); case MEMBERS -> membersTable(); case MINE -> mine(); } }

    private void factionTable() {
        boolean member = factions.factionIdFor(uiPlayer.getDbID()) != null;
        TableScrollView table = table(List.of("tc.factions.table.th.name", "tc.factions.table.th.members", "tc.factions.table.th.leader", "tc.factions.table.th.action"), List.of(32f, 20f, 26f, 22f));
        List<Faction> values = factions.factions();
        if (values.isEmpty()) table.addRow(textRow(text("tc.factions.ui.empty"), 100));
        for (Faction faction : values) {
            String members = factions.membersFor(faction.leaderDbId()).size() + " / " + factions.memberLimit(faction);
            boolean pending = factions.applicationsForPlayer(uiPlayer.getDbID()).stream().anyMatch(application -> application.factionId() == faction.id() && application.status() == FactionApplicationStatus.OPEN);
            TableCell action = member ? factions.factionIdFor(uiPlayer.getDbID()).equals(faction.id()) ? cell(text("tc.factions.table.member"), 22) : cell(text("tc.factions.ui.apply_unavailable"), 22) : pending ? cell(text("tc.factions.applicants.pending"), 22) : buttonCell(text("tc.factions.ui.apply"), 22,
                    () -> confirm(text("tc.factions.ui.apply_title"), text("tc.factions.ui.apply_confirm").replace("PH_NAME", faction.name()), () -> { factions.apply(uiPlayer.getDbID(), faction.id()); rebuild(); }));
            table.addRow(new TableRow(Arrays.asList(richCell("<color=" + faction.color() + ">" + faction.name() + "</color>", 32), cell(members, 20), cell(playerName(faction.leaderDbId()), 26), action)));
        }
        body.addChild(table);
    }

    private void applicationsTable() {
        TableScrollView table = table(List.of("tc.factions.table.th.faction", "tc.factions.table.th.status", "tc.factions.table.th.action"), List.of(42f, 25f, 33f));
        List<FactionApplication> values = factions.applicationsForPlayer(uiPlayer.getDbID());
        if (values.isEmpty()) table.addRow(textRow(text("tc.factions.ui.applications_empty"), 100));
        for (FactionApplication application : values) {
            String action = application.status() == FactionApplicationStatus.OPEN ? text("tc.factions.ui.withdraw") : text("tc.factions.ui.remove");
            Runnable task = application.status() == FactionApplicationStatus.OPEN
                    ? () -> { factions.withdraw(uiPlayer.getDbID(), application.id()); rebuild(); }
                    : () -> { factions.removeApplication(uiPlayer.getDbID(), application.id()); rebuild(); };
            table.addRow(new TableRow(Arrays.asList(cell(factions.factionName(application.factionId()), 42), cell(status(application.status()), 25), buttonCell(action, 33, task))));
        }
        body.addChild(table);
    }

    private void applicantsTable() {
        Integer factionId = factions.factionIdFor(uiPlayer.getDbID());
        if (factionId == null || !factions.canManageApplications(uiPlayer.getDbID())) { tab = Tab.MEMBERS; rebuild(); return; }
        TableScrollView table = table(List.of("tc.factions.table.th.player", "tc.factions.table.th.action"), List.of(48f, 52f));
        List<FactionApplication> values = factions.openApplications(factionId);
        if (values.isEmpty()) table.addRow(textRow(text("tc.factions.applicants.empty"), 100));
        for (FactionApplication application : values) {
            OZUIElement actions = new OZUIElement(); actions.setSize(210, 28, false);
            AdvancedButton accept = AdvancedButtonFactory.ok(text("tc.factions.ui.accept"), ignored -> confirm(text("tc.factions.ui.accept_title"), text("tc.factions.ui.accept_confirm").replace("PH_ID", playerName(application.playerDbId())), () -> { factions.accept(uiPlayer.getDbID(), application.id()); rebuild(); }));
            accept.setPivot(Pivot.MiddleLeft); accept.setPosition(0, 50, true); accept.setSize(96, 26, false); actions.addChild(accept);
            AdvancedButton reject = AdvancedButtonFactory.danger(text("tc.factions.ui.reject"), ignored -> confirm(text("tc.factions.ui.reject_title"), text("tc.factions.ui.reject_confirm").replace("PH_ID", playerName(application.playerDbId())), () -> { factions.reject(uiPlayer.getDbID(), application.id()); rebuild(); }));
            reject.setPivot(Pivot.MiddleRight); reject.setPosition(100, 50, true); reject.setSize(96, 26, false); actions.addChild(reject);
            table.addRow(new TableRow(Arrays.asList(cell(playerName(application.playerDbId()), 48), new TableCell(actions, 52))));
        }
        body.addChild(table);
    }

    private void membersTable() {
        List<FactionMember> members = factions.membersFor(uiPlayer.getDbID());
        Map<Integer, PlayerRecord> records = factions.playerRecords(members.stream().map(FactionMember::playerDbId).collect(java.util.stream.Collectors.toSet()));
        TableScrollView table = table(List.of("tc.factions.table.th.player", "tc.factions.table.th.last_seen", "tc.factions.table.th.joined", "tc.factions.table.th.role", "tc.factions.table.th.action"), List.of(22f, 19f, 19f, 18f, 22f));
        if (members.isEmpty()) table.addRow(textRow(text("tc.factions.table.members_empty"), 100));
        for (FactionMember member : members) table.addRow(new TableRow(Arrays.asList(cell(playerName(member.playerDbId(), records), 22), cell(lastSeen(member.playerDbId(), records), 19), cell(date(member.joinedAt()), 19), cell(role(member.role()), 18), memberActions(member))));
        body.addChild(table);
    }

    private TableCell memberActions(FactionMember member) {
        if (!factions.canManageMembers(uiPlayer.getDbID()) || member.playerDbId() == uiPlayer.getDbID() || member.role() == FactionRole.LEADER) return cell("", 22);
        OZUIElement actions = new OZUIElement(); actions.setSize(130, 28, false);
        if (factions.isLeader(uiPlayer.getDbID()) && member.role() == FactionRole.OFFICER) {
            AdvancedButton demote = AdvancedButtonFactory.defaultButton(text("tc.factions.members.action.demote"), ignored -> run(() -> { factions.changeRole(uiPlayer.getDbID(), member.playerDbId(), FactionRole.MEMBER); rebuild(); }));
            demote.setPivot(Pivot.MiddleLeft); demote.setPosition(0, 50, true); demote.setSize(34, 26, false); actions.addChild(demote);
            AdvancedButton transfer = AdvancedButtonFactory.ok(text("tc.factions.members.action.transfer_leadership"), ignored -> confirm(text("tc.factions.members.transfer_leadership_title"), text("tc.factions.members.transfer_leadership_confirm").replace("PH_PLAYER", playerName(member.playerDbId())), () -> { factions.changeRole(uiPlayer.getDbID(), member.playerDbId(), FactionRole.LEADER); rebuild(); }));
            transfer.setPivot(Pivot.MiddleLeft); transfer.setPosition(31, 50, true); transfer.setSize(34, 26, false); actions.addChild(transfer);
        }
        if (member.role() == FactionRole.NEWCOMER || (factions.isLeader(uiPlayer.getDbID()) && member.role() == FactionRole.MEMBER)) {
            AdvancedButton rank = AdvancedButtonFactory.defaultButton(text("tc.factions.members.action.promote"), ignored -> run(() -> { factions.changeRole(uiPlayer.getDbID(), member.playerDbId(), member.role() == FactionRole.NEWCOMER ? FactionRole.MEMBER : FactionRole.OFFICER); rebuild(); }));
            rank.setPivot(Pivot.MiddleLeft); rank.setPosition(0, 50, true); rank.setSize(34, 26, false); actions.addChild(rank);
        }
        AdvancedButton remove = AdvancedButtonFactory.danger(text("tc.factions.members.action.remove"), ignored -> confirm(text("tc.factions.ui.remove_title"), text("tc.factions.ui.remove_confirm").replace("PH_ID", playerName(member.playerDbId())), () -> { factions.remove(uiPlayer.getDbID(), member.playerDbId()); rebuild(); }));
        remove.setPivot(Pivot.MiddleRight); remove.setPosition(100, 50, true); remove.setSize(34, 26, false); actions.addChild(remove);
        return new TableCell(actions, 22);
    }

    private void create() {
        label(text("tc.factions.ui.create_name"), 16, 18, 300); UITextField name = field(16, 42, 310, 30, 20);
        label(text("tc.factions.ui.create_color"), 16, 88, 300); UITextField color = field(16, 112, 130, 30, 7); color.setReadOnly(true); color.setText("#FFFFFF");
        UILabel colorPreview = colorPreview("#FFFFFF", 158, 112, 168);
        button(text("tc.factions.ui.choose_color"), 16, 150, 210, () -> chooseColor(text("tc.factions.ui.create_color"), "#FFFFFF", selected -> { color.setText(selected); colorPreview.setText(colorText(selected)); }));
        label(text("tc.factions.ui.create_cost").replace("PH_PRICE", Long.toString(factions.foundationCost())).replace("PH_CURRENCY", factions.currencyIdentifier()), 16, 198, 650);
        label(text("tc.factions.ui.create_transfer"), 16, 226, 650);
        button(text("tc.factions.ui.create_submit"), 16, 273, 210, () -> name.getCurrentText(uiPlayer, n -> color.getCurrentText(uiPlayer, c -> confirm(text("tc.factions.ui.create_title"), text("tc.factions.ui.create_confirm").replace("PH_NAME", n == null ? "" : n), () -> { factions.create(uiPlayer, n, c); tab = Tab.MEMBERS; rebuild(); }))));
    }

    private void mine() {
        Integer factionId = factions.factionIdFor(uiPlayer.getDbID());
        Faction faction = factionId == null ? null : factions.factions().stream().filter(value -> value.id() == factionId).findFirst().orElse(null);
        if (faction == null) { tab = Tab.LIST; rebuild(); return; }
        label(text("tc.factions.details.name").replace("PH_NAME", faction.name()), 16, 18, 350);
        richLabel(text("tc.factions.details.color").replace("PH_COLOR", colorText(faction.color())), 16, 52, 350);
        label(text("tc.factions.details.members").replace("PH_COUNT", Integer.toString(factions.membersFor(uiPlayer.getDbID()).size())).replace("PH_LIMIT", Integer.toString(factions.memberLimit(faction))), 16, 86, 350);
        label(text("tc.factions.details.balance"), 16, 120, 350);
        int balanceY = 146;
        for (FactionService.FactionAccountBalance balance : factions.accountBalancesFor(uiPlayer.getDbID())) {
            String currency = balance.currencyName() == null || balance.currencyName().isBlank()
                    ? balance.currencyIdentifier()
                    : balance.currencyName() + " (" + balance.currencyIdentifier() + ")";
            label(currency + ": " + balance.balance(), 16, balanceY, 310);
            balanceY += 26;
        }
        int x = 340;
        if (factions.isLeader(uiPlayer.getDbID())) {
            button(text("tc.factions.details.change_color"), x, 18, 270, () -> chooseColor(text("tc.factions.details.change_color"), faction.color(), selected -> run(() -> { factions.updateColor(uiPlayer, selected); rebuild(); })));
        }
        label(text("tc.factions.details.deposit"), x, 98, 400); UITextField amount = field(x, 122, 150, 30, 12);
        String[] currency = { factions.currencyIdentifier() };
        List<DropdownOption> currencies = factions.currencies().stream().map(value -> new DropdownOption(value.identifier(), value.name().isBlank() ? value.identifier() : value.name() + " (" + value.identifier() + ")")).toList();
        Dropdown currencyPicker = new Dropdown(currencies, currency[0], value -> currency[0] = value);
        currencyPicker.setPivot(Pivot.UpperLeft); currencyPicker.setPosition(x + 162, 122, false); currencyPicker.setSize(280, 30, false); body.addChild(currencyPicker);
        button(text("tc.factions.details.deposit_submit"), x, 160, 130, () -> amount.getCurrentText(uiPlayer, value -> run(() -> { factions.contribute(uiPlayer, Long.parseLong(value), currency[0]); rebuild(); })));
        AdvancedButton leave = factions.isLeader(uiPlayer.getDbID()) ? disabledDanger(text("tc.factions.ui.leave")) : AdvancedButtonFactory.danger(text("tc.factions.ui.leave"), ignored -> confirm(text("tc.factions.ui.leave_title"), text("tc.factions.ui.leave_confirm"), () -> { factions.leave(uiPlayer); tab = Tab.LIST; rebuild(); }));
        leave.setPivot(Pivot.UpperLeft); leave.setPosition(x, 220, false); leave.setSize(150, 30, false); body.addChild(leave);
        if (factions.isLeader(uiPlayer.getDbID())) { AdvancedButton dissolve = AdvancedButtonFactory.danger(text("tc.factions.ui.dissolve"), ignored -> confirm(text("tc.factions.ui.dissolve_title"), text("tc.factions.ui.dissolve_confirm"), () -> { factions.dissolve(uiPlayer); tab = Tab.LIST; rebuild(); })); dissolve.setPivot(Pivot.UpperLeft); dissolve.setPosition(x + 165, 220, false); dissolve.setSize(150, 30, false); body.addChild(dissolve); }
    }

    private TableScrollView table(List<String> headers, List<Float> widths) { TableScrollView table = new TableScrollView(headers.stream().map(this::text).toList(), widths); table.setScrollBodyHeight(TABLE_HEIGHT); return table; }
    private TableRow textRow(String value, float width) { return new TableRow(List.of(cell(value, width))); }
    private TableCell cell(Object value, float width) { UILabel label = new UILabel(value == null ? "" : String.valueOf(value)); label.setFont(Font.Default); label.setFontSize(12); label.setTextAlign(TextAnchor.MiddleLeft); return new TableCell(label, width); }
    private TableCell richCell(String value, float width) { UILabel label = new UILabel(value); label.setRichTextEnabled(true); label.setFont(Font.Default); label.setFontSize(12); label.setTextAlign(TextAnchor.MiddleLeft); return new TableCell(label, width); }
    private TableCell buttonCell(String label, float width, Runnable action) { AdvancedButton button = AdvancedButtonFactory.defaultButton(label, ignored -> run(action)); button.setSize(130, 26, false); return new TableCell(button, width); }
    private AdvancedButton disabledDanger(String label) { AdvancedButton button = AdvancedButtonFactory.custom(new AdvancedButtonState(AdvancedBaseButton.State.DEFAULT, 0x00000080, 0xCC3333FF, 0xFFFFFFFF, 0x000000AA, 0xDD4444FF, label, null), new AdvancedButtonState(AdvancedBaseButton.State.DISABLED, 0x343434FF, 0x252525FF, 0x888888FF, null, null, label, null)); button.setState(AdvancedBaseButton.State.DISABLED); return button; }
    private String playerName(int id) { String value = Server.getLastKnownPlayerName(id); return value == null || value.isBlank() ? "#" + id : value; }
    private String playerName(int id, Map<Integer, PlayerRecord> records) { PlayerRecord record = records.get(id); return record == null || record.name == null || record.name.isBlank() ? playerName(id) : record.name; }
    private String lastSeen(int id, Map<Integer, PlayerRecord> records) { Player online = Server.getPlayerByDbID(id); if (online != null && online.isConnected()) return text("tc.factions.table.online"); PlayerRecord record = records.get(id); return record == null || record.lastSeenEpochSeconds <= 0 ? text("tc.factions.table.unknown") : date(record.lastSeenEpochSeconds * 1000L); }
    private String date(long epochMillis) { return DATE.format(Instant.ofEpochMilli(epochMillis)); }
    private String role(FactionRole value) { return text("tc.factions.table.role." + value.name().toLowerCase()); }
    private String status(FactionApplicationStatus value) { return text("tc.factions.table.status." + value.name().toLowerCase()); }
    private void confirm(String title, String message, Runnable action) { uiPlayer.showMessageBox(MessageBoxButtons.Yes_No, title, message, 0, answer -> { if (answer == 0) run(action); }); }
    private void run(Runnable action) { try { action.run(); } catch (IllegalArgumentException | IllegalStateException ex) { String message = ex.getMessage(); uiPlayer.showErrorMessageBox(text("tc.factions.ui.error_title"), message != null && message.startsWith("tc.") ? text(message) : message); } }
    private UITextField field(float x, float y, float width, float height, int max) { UITextField field = new UITextField(); field.setPivot(Pivot.UpperLeft); field.setPosition(x, y, false); field.setSize(width, height, false); field.setMaxCharacters(max); body.addChild(field); return field; }
    private void label(String value, float x, float y, float width) { UILabel label = new UILabel(value); label.setPivot(Pivot.UpperLeft); label.setPosition(x, y, false); label.setSize(width, 28, false); body.addChild(label); }
    private UILabel colorPreview(String value, float x, float y, float width) { return richLabel(colorText(value), x, y, width); }
    private UILabel richLabel(String value, float x, float y, float width) { UILabel label = new UILabel(value); label.setRichTextEnabled(true); label.setPivot(Pivot.UpperLeft); label.setPosition(x, y, false); label.setSize(width, 28, false); body.addChild(label); return label; }
    private void chooseColor(String title, String initialColor, java.util.function.Consumer<String> selected) { uiPlayer.showColorPicker(title, colorInt(initialColor), value -> { if (value != null) selected.accept(colorCodeFromPickerValue(value)); }); }
    public static String colorCodeFromPickerValue(int value) { return String.format("#%06X", (value >>> 8) & 0xFFFFFF); }
    private static int colorInt(String color) { try { return Integer.parseInt(color.replace("#", ""), 16); } catch (NumberFormatException ex) { return 0xFFFFFF; } }
    private static String colorText(String color) { return "<color=" + color + ">■ " + color + "</color>"; }
    private void button(String value, float x, float y, float width, Runnable action) { AdvancedButton button = AdvancedButtonFactory.defaultButton(value, ignored -> run(action)); button.setPivot(Pivot.UpperLeft); button.setPosition(x, y, false); button.setSize(width, 30, false); body.addChild(button); }
    private String text(String key) { return t().get(key, uiPlayer); }
}
