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
package forge.game.spellability;

import forge.game.Game;
import forge.game.GameType;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.cost.IndividualCostPaymentInstance;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.staticability.StaticAbilityCastWithFlash;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * SpellAbilityRestriction class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class SpellAbilityRestriction extends SpellAbilityVariables {
    // A class for handling SpellAbility Restrictions. These restrictions
    // include:
    // Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player,
    // Threshold, Metalcraft, LevelRange, etc
    // Each value will have a default, that can be overridden (mostly by
    // AbilityFactory)
    // The canPlay function will use these values to determine if the current
    // game state is ok with these restrictions

    /**
     * <p>
     * Constructor for SpellAbilityRestriction.
     * </p>
     */
    public SpellAbilityRestriction() {
    }

    /**
     * <p>
     * setRestrictions.
     * </p>
     *
     * @param params
     *            a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public final void setRestrictions(final Map<String, String> params) {
        if (params.containsKey("Activation")) {
            final String value = params.get("Activation");
            if (value.equals("Threshold")) {
                this.setThreshold(true);
            }
            if (value.equals("Metalcraft")) {
                this.setMetalcraft(true);
            }
            if (value.equals("Delirium")) {
                this.setDelirium(true);
            }
            if (value.equals("Hellbent")) {
                this.setHellbent(true);
            }
            if (value.equals("Desert")) {
                this.setDesert(true);
            }
            if (value.equals("Blessing")) {
                this.setBlessing(true);
            }
        }

        if (params.containsKey("ActivationZone")) {
            this.setZone(ZoneType.smartValueOf(params.get("ActivationZone")));
        }

        if (params.containsKey("SorcerySpeed")) {
            this.setSorcerySpeed(true);
        }

        if (params.containsKey("InstantSpeed")) {
            this.setInstantSpeed(true);
        }

        if (params.containsKey("PlayerTurn")) {
            this.setPlayerTurn(true);
        }

        if (params.containsKey("OpponentTurn")) {
            this.setOpponentTurn(true);
        }

        if (params.containsKey("Activator")) {
            this.setActivator(params.get("Activator"));
        }

        if (params.containsKey("ActivationLimit")) {
            this.setLimitToCheck(params.get("ActivationLimit"));
        }

        if (params.containsKey("GameActivationLimit")) {
            this.setGameLimitToCheck(params.get("GameActivationLimit"));
        }

        if (params.containsKey("ActivationPhases")) {
            this.setPhases(PhaseType.parseRange(params.get("ActivationPhases")));
        }

        if (params.containsKey("ActivationGameTypes")) {
            this.setGameTypes(GameType.listValueOf(params.get("ActivationGameTypes")));
        }

        if (params.containsKey("ActivationCardsInHand")) {
            this.setActivateCardsInHand(Integer.parseInt(params.get("ActivationCardsInHand")));
        }

        if (params.containsKey("ActivationChosenColor")) {
            this.setColorToCheck(params.get("ActivationChosenColor"));
        }

        if (params.containsKey("IsPresent")) {
            this.setIsPresent(params.get("IsPresent"));
            if (params.containsKey("PresentCompare")) {
                this.setPresentCompare(params.get("PresentCompare"));
            }
            if (params.containsKey("PresentZone")) {
                this.setPresentZone(ZoneType.smartValueOf(params.get("PresentZone")));
            }
        }

        if (params.containsKey("PresentDefined")) {
            this.setPresentDefined(params.get("PresentDefined"));
        }
        if (params.containsKey("IsNotPresent")) {
            this.setIsPresent(params.get("IsNotPresent"));
            this.setPresentCompare("EQ0");
        }

        // basically PresentCompare for life totals:
        if (params.containsKey("ActivationLifeTotal")) {
            this.setLifeTotal(params.get("ActivationLifeTotal"));
            if (params.containsKey("ActivationLifeAmount")) {
                this.setLifeAmount(params.get("ActivationLifeAmount"));
            }
        }

        if (params.containsKey("CheckSVar")) {
            this.setSvarToCheck(params.get("CheckSVar"));
        }
        if (params.containsKey("SVarCompare")) {
            this.setSvarOperator(params.get("SVarCompare").substring(0, 2));
            this.setSvarOperand(params.get("SVarCompare").substring(2));
        }
    } // end setRestrictions()

    /**
     * <p>
     * checkZoneRestrictions.
     * </p>
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean checkZoneRestrictions(final Card c, final SpellAbility sa) {

        final Player activator = sa.getActivatingPlayer();
        final Zone cardZone = c.getLastKnownZone();
        Card cp = c;

        // for Bestow need to check the animated State
        if (sa.isSpell() && sa.isBestow()) {
            // already bestowed or in battlefield, no need to check for spell
            if (c.isInZone(ZoneType.Battlefield)) {
                return false;
            }

            // if card is lki and bestowed, then do nothing there, it got already animated
            if (!(c.isLKI() && c.isBestowed())) {
                if (!c.isLKI()) {
                    cp = CardUtil.getLKICopy(c);
                }

                if (!cp.isBestowed()) {
                    cp.animateBestow(!cp.isLKI());
                }
            }
        }

        if (cardZone == null || this.getZone() == null || !cardZone.is(this.getZone())) {
            // If Card is not in the default activating zone, do some additional checks

            // A conspiracy with hidden agenda: reveal at any time
            if (cardZone != null && cardZone.is(ZoneType.Command) && sa.hasParam("HiddenAgenda")) {
                return true;
            }
            // Not a Spell, or on Battlefield, return false
            if (!sa.isSpell() || (cardZone != null && ZoneType.Battlefield.equals(cardZone.getZoneType()))
                    || (this.getZone() != null && !this.getZone().equals(ZoneType.Hand))) {
                return false;
            }
            if (cardZone != null && cardZone.is(ZoneType.Stack)) {
                return false;
            }
            if (sa.isSpell()) {
                final CardPlayOption o = c.mayPlay(sa.getMayPlay());
                if (o == null) {
                    return this.getZone() == null || (cardZone != null && cardZone.is(this.getZone()));
                } else if (o.getPlayer() == activator) {
                    Map<String,String> params = sa.getMayPlay().getMapParams();

                    // NOTE: this assumes that it's always possible to cast cards from hand and you don't
                    // need special permissions for that. If WotC ever prints a card that forbids casting
                    // cards from hand, this may become relevant.
                    if (!o.grantsZonePermissions() && cardZone != null && !cardZone.is(ZoneType.Hand)) {
                        final List<CardPlayOption> opts = c.mayPlay(activator);
                        boolean hasOtherGrantor = false;
                        for (CardPlayOption opt : opts) {
                            if (opt.grantsZonePermissions()) {
                                hasOtherGrantor = true;
                                break;
                            }
                        }
                        if (cardZone.is(ZoneType.Graveyard) && sa.isAftermath()) {
                            // Special exclusion for Aftermath, useful for e.g. As Foretold
                            return true;
                        }
                        if (!hasOtherGrantor) {
                            return false;
                        }
                    }

                    if (params.containsKey("Affected")) {
                        if (!cp.isValid(params.get("Affected").split(","), activator, o.getHost(), null)) {
                            return false;
                        }
                    }

                    if (params.containsKey("ValidSA")) {
                        if (!sa.isValid(params.get("ValidSA").split(","), activator, o.getHost(), null)) {
                            return false;
                        }
                    }

                    // TODO: this is an exception for Aftermath. Needs to be somehow generalized.
                    if (this.getZone() != ZoneType.Graveyard && sa.isAftermath() && sa.getCardState() != null) {
                        return false;
                    }

                    return true;
                }
            }
            return false;
        }

        return true;
    }

    /**
     * <p>
     * checkTimingRestrictions.
     * </p>
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean checkTimingRestrictions(final Card c, final SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();

        if (this.isPlayerTurn() && !game.getPhaseHandler().isPlayerTurn(activator)) {
            return false;
        }

        if (this.isOpponentTurn() && !game.getPhaseHandler().getPlayerTurn().isOpponentOf(activator)) {
            return false;
        }

        if (this.getPhases().size() > 0) {
            if (!this.getPhases().contains(game.getPhaseHandler().getPhase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * checkActivatorRestrictions.
     * </p>
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean checkActivatorRestrictions(final Card c, final SpellAbility sa) {
        Player activator = sa.getActivatingPlayer();

        if (sa.isSpell()) {
            // Spells should always default to "controller" but use mayPlay check.
            final CardPlayOption o = c.mayPlay(sa.getMayPlay());
            if (o != null && o.getPlayer() == activator) {
                return true;
            }
        }

        String validPlayer = this.getActivator();
        return activator.isValid(validPlayer, c.getController(), c, sa);
    }

    public final boolean checkOtherRestrictions(final Card c, final SpellAbility sa, final Player activator) {
        final Game game = activator.getGame();

        // legendary sorcery
        if (c.isSorcery() && c.getType().isLegendary() && CardLists.getValidCardCount(
                activator.getCardsIn(ZoneType.Battlefield),
                "Creature.Legendary,Planeswalker.Legendary", c.getController(), c) <= 0) {
            return false;
        }

        // Explicit Aftermath check there
        if (sa.isAftermath() && !c.isInZone(ZoneType.Graveyard)) {
            return false;
        }

        if (getCardsInHand() != -1) {
            if (activator.getCardsIn(ZoneType.Hand).size() != getCardsInHand()) {
                return false;
            }
        }

        if (getColorToCheck() != null) {
            if (!sa.getHostCard().hasChosenColor(getColorToCheck())) {
                return false;
            }
        }
        if (isHellbent()) {
            if (!activator.hasHellbent()) {
                return false;
            }
        }
        if (isThreshold()) {
            if (!activator.hasThreshold()) {
                return false;
            }
        }
        if (isMetalcraft()) {
            if (!activator.hasMetalcraft()) {
                return false;
            }
        }
        if (isDelirium()) {
            if (!activator.hasDelirium()) {
                return false;
            }
        }
        if (sa.isSurged()) {
            if (!activator.hasSurge()) {
                return false;
            }
        }
        if (sa.isSpectacle()) {
            if (activator.getOpponentLostLifeThisTurn() <= 0) {
                return false;
            }
        }
        if (isDesert()) {
            if (!activator.hasDesert()) {
                return false;
            }
        }
        if (isBlessing()) {
            if (!activator.hasBlessing()) {
                return false;
            }
        }
        if (sa.isProwl()) {
            boolean prowlFlag = false;
            for (final String type : c.getType().getCreatureTypes()) {
                if (activator.hasProwl(type)) {
                    prowlFlag = true;
                }
            }
            if (!prowlFlag) {
                return false;
            }
        }
        if (this.getIsPresent() != null) {
            CardCollectionView list;
            if (this.getPresentDefined() != null) {
                list = AbilityUtils.getDefinedCards(sa.getHostCard(), this.getPresentDefined(), sa);
            } else {
                list = game.getCardsIn(this.getPresentZone());
            }

            list = CardLists.getValidCards(list, this.getIsPresent().split(","), activator, c, sa);

            int right = 1;
            final String rightString = this.getPresentCompare().substring(2);
            right = AbilityUtils.calculateAmount(c, rightString, sa);
            final int left = list.size();

            if (!Expressions.compare(left, this.getPresentCompare(), right)) {
                return false;
            }
        }

        if (this.getLifeTotal() != null) {
            int life = 1;
            if (this.getLifeTotal().equals("You")) {
                life = activator.getLife();
            }
            if (this.getLifeTotal().equals("OpponentSmallest")) {
                life = activator.getOpponentsSmallestLifeTotal();
            }

            int right = 1;
            final String rightString = this.getLifeAmount().substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(sa.getHostCard(), sa.getHostCard().getSVar("X"));
            } else {
                right = Integer.parseInt(this.getLifeAmount().substring(2));
            }

            if (!Expressions.compare(life, this.getLifeAmount(), right)) {
                return false;
            }
        }

        if (sa.isPwAbility()) {
            if (!c.hasKeyword("CARDNAME's loyalty abilities can be activated at instant speed.")
                    && !activator.canCastSorcery()) {
                return false;
            }

            final int initialLimit = c.hasKeyword("CARDNAME's loyalty abilities can be activated twice each turn rather than only once") ? 1 : 0;
            final int limits = c.getAmountOfKeyword("May activate CARDNAME's loyalty abilities once") + initialLimit;

            int numActivates = c.getPlaneswalkerAbilityActivated();
            if (numActivates > limits) {
                return false;
            }
        }

        if (sa.isBoast()) {
            int limit = activator.hasKeyword("Creatures you control can boast twice during each of your turns rather than once.") ? 2 : 1;
            if (limit <= sa.getActivationsThisTurn()) {
                return false;
            }
        }

        // Rule 605.3c about Mana Abilities
        if (sa.isManaAbility()) {
            for (IndividualCostPaymentInstance i : game.costPaymentStack) {
                if (i.getPayment().getAbility().equals(sa)) {
                    return false;
                }
            }
        }

        if (this.getsVarToCheck() != null) {
            final int svarValue = AbilityUtils.calculateAmount(c, this.getsVarToCheck(), sa);
            final int operandValue = AbilityUtils.calculateAmount(c, this.getsVarOperand(), sa);

            if (!Expressions.compare(svarValue, this.getsVarOperator(), operandValue)) {
                return false;
            }
        }

        if (this.getsVarToCheck() != null) {
            final int svarValue = AbilityUtils.calculateAmount(sa.getHostCard(), this.getsVarToCheck(), sa);
            final int operandValue = AbilityUtils.calculateAmount(sa.getHostCard(), this.getsVarOperand(), sa);

            if (!Expressions.compare(svarValue, this.getsVarOperator(), operandValue)) {
                return false;
            }
        }
    	return true;
    }

    /**
     * <p>
     * canPlay.
     * </p>
     *
     * @param c
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean canPlay(final Card c, final SpellAbility sa) {
        if (c.isPhasedOut() || c.isUsedToPay()) {
            return false;
        }

        Player activator = sa.getActivatingPlayer();
        if (activator == null) {
            activator = c.getController();
            sa.setActivatingPlayer(activator);
            System.out.println(c.getName() + " Did not have activator set in SpellAbilityRestriction.canPlay()");
        }

        if (!StaticAbilityCastWithFlash.anyWithFlashNeedsTargeting(sa, c, activator)) {
            if (!sa.canCastTiming(c, activator)) {
                return false;
            }
        }

        if (!sa.hasSVar("IsCastFromPlayEffect")) {
            if (!checkTimingRestrictions(c, sa)) {
                return false;
            }

            if (!checkActivatorRestrictions(c, sa)) {
                return false;
            }
        }

        if (!checkZoneRestrictions(c, sa)) {
            return false;
        }

        if (!checkOtherRestrictions(c, sa, activator)) {
            return false;
        }

        if (this.getLimitToCheck() != null) {
            String limit = this.getLimitToCheck();
            int activationLimit = AbilityUtils.calculateAmount(c, limit, sa);
            this.setActivationLimit(activationLimit);

            if ((this.getActivationLimit() != -1) && (sa.getActivationsThisTurn() >= this.getActivationLimit())) {
                return false;
            }
        }

        if (this.getGameLimitToCheck() != null) {
            String limit = this.getGameLimitToCheck();
            int gameActivationLimit = AbilityUtils.calculateAmount(c, limit, sa);
            this.setGameActivationLimit(gameActivationLimit);

            if ((this.getGameActivationLimit() != -1) && (sa.getActivationsThisGame() >= this.getGameActivationLimit())) {
                return false;
            }
        }

        return true;
    } // canPlay()

} // end class SpellAbilityRestriction
