/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gui.deckeditor.controllers;

// import java.awt.Font;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Supplier;

import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.deckeditor.SEditorUtil;
import forge.gui.deckeditor.tables.DeckController;
import forge.gui.deckeditor.tables.EditorTableView;
import forge.gui.deckeditor.tables.SColumnUtil;
import forge.gui.deckeditor.tables.SColumnUtil.ColumnName;
import forge.gui.deckeditor.tables.TableColumnInfo;
import forge.gui.deckeditor.views.VAllDecks;
import forge.gui.deckeditor.views.VCardCatalog;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.deckeditor.views.VDeckgen;
import forge.gui.framework.DragCell;
import forge.gui.home.quest.CSubmenuQuestDecks;
import forge.gui.toolbox.FLabel;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.quest.QuestController;

//import forge.quest.data.QuestBoosterPack;

/**
 * Child controller for quest deck editor UI.
 * <br><br>
 * Card catalog and decks are drawn from a QuestController object.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 * 
 * @author Forge
 * @version $Id$
 */
public final class CEditorQuest extends ACEditorBase<CardPrinted, Deck> {
    private final QuestController questData;
    private final DeckController<Deck> controller;
    private DragCell allDecksParent = null;
    private DragCell deckGenParent = null;
    private boolean sideboardMode = false;

    private Map<CardPrinted, Integer> decksUsingMyCards;

    private final Function<Entry<InventoryItem, Integer>, Comparable<?>> fnDeckCompare = new Function<Entry<InventoryItem, Integer>, Comparable<?>>() {
        @Override
        public Comparable<?> apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = decksUsingMyCards.get(from.getKey());
            return iValue == null ? Integer.valueOf(0) : iValue;
        }
    };

    private final Function<Entry<InventoryItem, Integer>, Object> fnDeckGet = new Function<Entry<InventoryItem, Integer>, Object>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = decksUsingMyCards.get(from.getKey());
            return iValue == null ? "" : iValue.toString();
        }
    };

    /**
     * Child controller for quest deck editor UI.
     * <br><br>
     * Card catalog and decks are drawn from a QuestController object.
     * 
     * @param questData0 &emsp; {@link forge.quest.QuestController}
     */
    public CEditorQuest(final QuestController questData0) {
        this.questData = questData0;

        final EditorTableView<CardPrinted> tblCatalog = new EditorTableView<CardPrinted>(false, CardPrinted.class);
        final EditorTableView<CardPrinted> tblDeck = new EditorTableView<CardPrinted>(false, CardPrinted.class);

        tblCatalog.setAlwaysNonUnique(true);
        tblDeck.setAlwaysNonUnique(true);

        VCardCatalog.SINGLETON_INSTANCE.setTableView(tblCatalog.getTable());
        VCurrentDeck.SINGLETON_INSTANCE.setTableView(tblDeck.getTable());

        this.setTableCatalog(tblCatalog);
        this.setTableDeck(tblDeck);

        final Supplier<Deck> newCreator = new Supplier<Deck>() {
            @Override
            public Deck get() {
                return new Deck();
            }
        };

        this.controller = new DeckController<Deck>(questData0.getMyDecks(), this, newCreator);
    }

    /**
     * Adds any card to the catalog and data pool.
     * 
     * @param card {@link forge.item.CardPrinted}
     */
    public void addCheatCard(final CardPrinted card, int qty) {
        this.getTableCatalog().addCard(card, qty);
        this.questData.getCards().getCardpool().add(card, qty);
    }

    // fills number of decks using each card
    private Map<CardPrinted, Integer> countDecksForEachCard() {
        final Map<CardPrinted, Integer> result = new HashMap<CardPrinted, Integer>();
        for (final Deck deck : this.questData.getMyDecks()) {
            for (final Entry<CardPrinted, Integer> e : deck.getMain()) {
                final CardPrinted card = e.getKey();
                final Integer amount = result.get(card);
                result.put(card, Integer.valueOf(amount == null ? 1 : 1 + amount.intValue()));
            }
        }
        return result;
    }

    //=========== Overridden from ACEditorBase

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#addCard()
     */
    @Override
    public void addCard(InventoryItem item, boolean toAlternate, int qty) {
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        if (toAlternate) {
            // if we're in sideboard mode, the library will get adjusted properly when we call resetTables()
            if (!sideboardMode) {
                controller.getModel().getOrCreate(DeckSection.Sideboard).add(card, qty);
            }
        } else {
            getTableDeck().addCard(card, qty);
        }
        this.getTableCatalog().removeCard(card, qty);
        this.controller.notifyModelChanged();
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#removeCard()
     */
    @Override
    public void removeCard(InventoryItem item, boolean toAlternate, int qty) {
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        if (toAlternate) {
            // if we're in sideboard mode, the library will get adjusted properly when we call resetTables()
            if (!sideboardMode) {
                controller.getModel().getOrCreate(DeckSection.Sideboard).add(card, qty);
            }
        } else {
            this.getTableCatalog().addCard(card, qty);
        }
        this.getTableDeck().removeCard(card, qty);
        this.controller.notifyModelChanged();
    }

    @Override
    public void buildAddContextMenu(ContextMenuBuilder cmb) {
        cmb.addMoveItems(sideboardMode ? "Move" : "Add", "card", "cards", sideboardMode ? "to sideboard" : "to deck");
        cmb.addMoveAlternateItems(sideboardMode ? "Remove" : "Add", "card", "cards", sideboardMode ? "from deck" : "to sideboard");
        cmb.addTextFilterItem();
    }
    
    @Override
    public void buildRemoveContextMenu(ContextMenuBuilder cmb) {
        cmb.addMoveItems(sideboardMode ? "Move" : "Remove", "card", "cards", sideboardMode ? "to deck" : "from deck");
        cmb.addMoveAlternateItems(sideboardMode ? "Remove" : "Move", "card", "cards", sideboardMode ? "from sideboard" : "to sideboard");
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#updateView()
     */
    @Override
    public void resetTables() {
        final Deck deck = this.controller.getModel();

        final ItemPool<CardPrinted> cardpool = new ItemPool<CardPrinted>(CardPrinted.class);
        cardpool.addAll(this.questData.getCards().getCardpool());
        // remove bottom cards that are in the deck from the card pool
        cardpool.removeAll(deck.getMain());
        // remove sideboard cards from the catalog
        cardpool.removeAll(deck.getOrCreate(DeckSection.Sideboard));
        // show cards, makes this user friendly
        this.getTableCatalog().setDeck(cardpool);
        this.getTableDeck().setDeck(deck.getMain());
    }

    //=========== Overridden from ACEditorBase

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.ACEditorBase#getController()
     */
    @Override
    public DeckController<Deck> getDeckController() {
        return this.controller;
    }

    /**
     * Switch between the main deck and the sideboard editor.
     */
    public void switchEditorMode(boolean isSideboarding) {
        if (isSideboarding) {
            this.getTableCatalog().setDeck(this.controller.getModel().getMain());
            this.getTableDeck().setDeck(this.controller.getModel().getOrCreate(DeckSection.Sideboard));
        } else {
            resetTables();
        }

        VCardCatalog.SINGLETON_INSTANCE.getTabLabel().setText(isSideboarding ? "Main Deck" : "Card Catalog");
        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnPrintProxies().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setVisible(!isSideboarding);
        VCurrentDeck.SINGLETON_INSTANCE.getLblTitle().setText(isSideboarding ? "Sideboard" : "Title:");
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.ACEditorBase#show(forge.Command)
     */
    @SuppressWarnings("serial")
    @Override
    public void init() {
        final List<TableColumnInfo<InventoryItem>> columnsCatalog = SColumnUtil.getCatalogDefaultColumns();
        final List<TableColumnInfo<InventoryItem>> columnsDeck = SColumnUtil.getDeckDefaultColumns();

        this.decksUsingMyCards = this.countDecksForEachCard();

        // Add "new" column in catalog and deck
        columnsCatalog.add(SColumnUtil.getColumn(ColumnName.CAT_NEW));
        columnsCatalog.get(columnsCatalog.size() - 1).setSortAndDisplayFunctions(
                this.questData.getCards().getFnNewCompare(),
                this.questData.getCards().getFnNewGet());

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_NEW));
        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.questData.getCards().getFnNewCompare(),
                this.questData.getCards().getFnNewGet());

        columnsDeck.add(SColumnUtil.getColumn(ColumnName.DECK_DECKS));
        columnsDeck.get(columnsDeck.size() - 1).setSortAndDisplayFunctions(
                this.fnDeckCompare, this.fnDeckGet);

        this.getTableCatalog().setup(VCardCatalog.SINGLETON_INSTANCE, columnsCatalog);
        this.getTableDeck().setup(VCurrentDeck.SINGLETON_INSTANCE, columnsDeck);

        Deck deck = new Deck();

        SEditorUtil.resetUI();

        VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().setVisible(true);
        VCurrentDeck.SINGLETON_INSTANCE.getBtnDoSideboard().setVisible(true);
        ((FLabel) VCurrentDeck.SINGLETON_INSTANCE.getBtnDoSideboard()).setCommand(new Command() {
            @Override
            public void run() {
                sideboardMode = !sideboardMode;
                switchEditorMode(sideboardMode);
        } });
        
        deckGenParent = removeTab(VDeckgen.SINGLETON_INSTANCE);
        allDecksParent = removeTab(VAllDecks.SINGLETON_INSTANCE);        

        this.getDeckController().setModel(deck);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.controllers.ACEditorBase#exit()
     */
    @Override
    public boolean exit() {
        final boolean okToExit = SEditorIO.confirmSaveChanges();
        if (okToExit) {
            Singletons.getModel().getQuest().save();
            CSubmenuQuestDecks.SINGLETON_INSTANCE.update();
            //Re-add tabs
            if (deckGenParent != null) {
                deckGenParent.addDoc(VDeckgen.SINGLETON_INSTANCE);
            }
            if (allDecksParent != null) {
                allDecksParent.addDoc(VAllDecks.SINGLETON_INSTANCE);
            }
            
        }
        return okToExit;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param d0
     */
    public void load(Deck deck) {
        controller.setModel(deck);
    }
}
