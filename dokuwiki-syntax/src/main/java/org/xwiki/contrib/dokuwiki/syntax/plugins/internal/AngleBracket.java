package org.xwiki.contrib.dokuwiki.syntax.plugins.internal;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.dokuwiki.syntax.DokuWikiSyntaxParserHelper;
import org.xwiki.contrib.dokuwiki.syntax.plugins.DokuWikiPlugin;
import org.xwiki.contrib.dokuwiki.syntax.plugins.internal.angleBrackets.DokuWikiAngleBracketPlugin;
import org.xwiki.contrib.dokuwiki.syntax.plugins.internal.curlyBrackets.DokuWikiCurlyBracketPlugin;
import org.xwiki.rendering.listener.Listener;

/**
 * DokuWiki Angle plugin parser.
 *
 * @version $Id: $
 * @since 1.2
 */
@Component
@Named("angleBracket")
@Singleton
public class AngleBracket implements DokuWikiPlugin
{
    @Inject
    private DokuWikiSyntaxParserHelper helper;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    private List<DokuWikiAngleBracketPlugin> componentList;

    @Override public void parse(ArrayList<Character> buffer, Reader source, Listener listener)
            throws IOException, ComponentLookupException
    {
        // TODO : add support
        List<DokuWikiAngleBracketPlugin> componentList =
                componentManagerProvider.get().getInstanceList(DokuWikiAngleBracketPlugin.class);
        for (DokuWikiAngleBracketPlugin plugin : componentList) {
            plugin.parse(buffer, source, listener);
        }

        return;
    }
}
