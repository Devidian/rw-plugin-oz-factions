package de.omegazirkel.risingworld.playerfactions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;
import de.omegazirkel.risingworld.tools.I18n;

/** Optional reflection-only integration with the context-offer API introduced by OZ - Shop. */
public final class FactionShopBridge {
    private static final String OWNER = "OZPlayerFactions";
    private final Plugin owner;
    private final FactionService factions;
    public FactionShopBridge(Plugin owner, FactionService factions) { this.owner=owner; this.factions=factions; }

    public void registerOffers() {
        Plugin shop=owner.getPluginByName("OZ - Shop"); if(shop==null) return;
        try {
            ClassLoader loader=shop.getClass().getClassLoader();
            Class<?> callback=Class.forName("de.omegazirkel.risingworld.shop.ShopPurchaseContextCallback",true,loader);
            Class<?> policy=Class.forName("de.omegazirkel.risingworld.shop.ShopPurchasePolicy",true,loader);
            Class<?> resolver=Class.forName("de.omegazirkel.risingworld.shop.ShopPriceResolver",true,loader);
            Class<?> localization=Class.forName("de.omegazirkel.risingworld.shop.ShopOfferLocalization",true,loader);
            Method register=shop.getClass().getMethod("registerContextOffer",String.class,String.class,String.class,long.class,
                    String.class,String.class,String.class,String.class,String.class,callback,policy,resolver,localization);
            String currency=factions.currencyIdentifier(); if(currency==null||currency.isBlank()) return;
            for(FactionExtra extra:FactionExtra.values()) register.invoke(shop, extra.shopOfferId(), title(extra, null),
                    description(extra, null), 0L, currency, extra.shopOfferId(), "Factions", "OZ - Factions", OWNER,
                    proxy(loader,callback,(method,args)->complete(args[0])), proxy(loader,policy,(method,args)->authorize((Player)args[0])),
                    proxy(loader,resolver,(method,args)->price((Player)args[0],extra)),
                    proxy(loader,localization,(method,args)->method.getName().equals("title") ? title(extra,(Player)args[0]) : description(extra,(Player)args[0])));
            de.omegazirkel.risingworld.PlayerFactions.logger().info("Registered faction extras with OZ - Shop.");
        } catch (ReflectiveOperationException | LinkageError ex) {
            de.omegazirkel.risingworld.PlayerFactions.logger().error("OZ - Shop context-offer API is unavailable; faction extras were not registered.");
        }
    }
    private Object complete(Object context) throws Exception {
        Method player=context.getClass().getMethod("player"), offer=context.getClass().getMethod("offer"), price=context.getClass().getMethod("price"), correlation=context.getClass().getMethod("correlationId");
        Player buyer=(Player)player.invoke(context); Object shopOffer=offer.invoke(context);
        String id=(String)shopOffer.getClass().getMethod("getId").invoke(shopOffer);
        FactionExtra extra=extraFor(id); factions.completeExtraPurchase(buyer.getDbID(),extra,(Long)price.invoke(context),(String)correlation.invoke(context));
        Class<?> result=Class.forName("de.omegazirkel.risingworld.shop.ShopPurchaseResult",true,context.getClass().getClassLoader());
        return result.getMethod("success",String.class,shopOffer.getClass()).invoke(null,text("tc.factions.shop.success",buyer),shopOffer);
    }
    private Object authorize(Player player) throws Exception {
        if(player==null) return authorization("deny",text("tc.factions.shop.error.leader",null));
        Integer factionId=factions.factionIdFor(player.getDbID());
        if(factionId==null || !factions.isLeader(player.getDbID())) return authorization("deny",text("tc.factions.shop.error.leader",player));
        String account=factions.accountIdForFaction(factionId); if(account==null||account.isBlank()) return authorization("deny",text("tc.factions.shop.error.account",player));
        return authorization("allowSystem",account,OWNER);
    }
    private Object authorization(String method,String... values) throws Exception {
        Plugin shop=owner.getPluginByName("OZ - Shop"); Class<?> type=Class.forName("de.omegazirkel.risingworld.shop.ShopPurchaseAuthorization",true,shop.getClass().getClassLoader());
        Class<?>[] types=new Class<?>[values.length]; java.util.Arrays.fill(types,String.class);
        return type.getMethod(method,types).invoke(null,(Object[])values);
    }
    private String title(FactionExtra extra, Player player) { return text("tc.factions.shop."+extra.key()+".title",player); }
    private String description(FactionExtra extra, Player player) { return text("tc.factions.shop."+extra.key()+".description",player); }
    private String text(String key,Player player) { return player == null ? I18n.getInstance(owner).get(key,"en") : I18n.getInstance(owner).get(key,player); }
    private long price(Player player,FactionExtra extra) { return player==null ? -1L : factions.factionIdFor(player.getDbID())==null ? -1L : factions.extraPrice(factions.factionIdFor(player.getDbID()),extra); }
    private static FactionExtra extraFor(String id) { for(FactionExtra e:FactionExtra.values()) if(e.shopOfferId().equalsIgnoreCase(id)) return e; throw new IllegalArgumentException("Unknown faction extra."); }
    private static Object proxy(ClassLoader loader,Class<?> type,ThrowingCall call) { InvocationHandler h=(proxy,method,args)->method.getName().equals("toString")?type.getSimpleName():call.call(method,args); return Proxy.newProxyInstance(loader,new Class<?>[]{type},h); }
    @FunctionalInterface private interface ThrowingCall { Object call(Method method,Object[] args) throws Exception; }
}
