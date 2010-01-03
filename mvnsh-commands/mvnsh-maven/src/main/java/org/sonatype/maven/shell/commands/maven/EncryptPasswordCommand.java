/*
 * Copyright (C) 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sonatype.maven.shell.commands.maven;

import com.google.inject.Inject;
import org.sonatype.gshell.command.Command;
import org.sonatype.gshell.command.CommandActionSupport;
import org.sonatype.gshell.command.CommandContext;
import org.sonatype.gshell.command.IO;
import org.sonatype.gshell.plexus.PlexusRuntime;
import org.sonatype.gshell.util.cli.Argument;
import org.sonatype.gshell.util.cli.Option;
import org.sonatype.gshell.util.pref.Preferences;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * Encrypt passwords.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 0.9
 */
@Command(name = "encrypt-password")
@Preferences(path = "commands/encrypt-password")
public class EncryptPasswordCommand
    extends CommandActionSupport
{
    private final PlexusRuntime plexus;

    @Option(name="-m", aliases={"--master"})
    private boolean master;

    @Argument(required=true)
    private String passwd;

    @Inject
    public EncryptPasswordCommand(final PlexusRuntime plexus) {
        assert plexus != null;
        this.plexus = plexus;
    }

    public Object execute(final CommandContext context) throws Exception {
        assert context != null;
        IO io = context.getIo();

        DefaultSecDispatcher dispatcher = (DefaultSecDispatcher) plexus.lookup(SecDispatcher.class);
        String result;

        if (master) {
            DefaultPlexusCipher cipher = new DefaultPlexusCipher();
            result = cipher.encryptAndDecorate(passwd, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
        }
        else {
            String configurationFile = dispatcher.getConfigurationFile();

            if (configurationFile.startsWith("~")) {
                configurationFile = System.getProperty("user.home") + configurationFile.substring(1);
            }

            String file = System.getProperty(DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION, configurationFile);

            String master = null;

            SettingsSecurity sec = SecUtil.read(file, true);
            if (sec != null) {
                master = sec.getMaster();
            }

            if (master == null) {
                throw new IllegalStateException("Master password is not set in the setting security file: " + file);
            }

            DefaultPlexusCipher cipher = new DefaultPlexusCipher();
            String masterPasswd = cipher.decryptDecorated(master, DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);

            result = cipher.encryptAndDecorate(passwd, masterPasswd);
        }

        io.out.println(result);

        return result;
    }
}