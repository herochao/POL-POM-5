/*
 * Copyright (C) 2015 PÂRIS Quentin
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.playonlinux.installer;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.python.core.PyException;

import com.playonlinux.framework.ScriptFailureException;
import com.playonlinux.python.Interpreter;
import com.playonlinux.services.BackgroundService;

public abstract class Script implements BackgroundService {
    private static final Logger LOGGER = Logger.getLogger(Script.class);

    private Thread scriptThread;
    private final String scriptContent;

    protected Script(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    public static Script.Type detectScriptType(String script) {
        String firstLine = script.split("\n")[0];
        if("#!/bin/bash".equals(firstLine) || "#!/usr/bin/env playonlinux-bash".equals(firstLine)) {
            return Script.Type.LEGACY;
        } else {
            return Script.Type.RECENT;
        }
    }


    @Override
    public void shutdown() {
        scriptThread.interrupt();
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public enum Type {
        RECENT,
        LEGACY
    }


    @Override
    public void start() {
        scriptThread = new Thread() {
            @Override
            public void run() {
                try {
                    executeInterpreter();
                } catch (PyException e) {
                    if(e.getCause() instanceof ScriptFailureException) {
                        LOGGER.error("The script encountered an error");
                    }
                    if(e.getCause() instanceof CancelException) {
                        LOGGER.info("The script has been canceled");
                    }
                    LOGGER.error(ExceptionUtils.getStackTrace(e));
                } catch (ScriptFailureException e) {
                    LOGGER.error("The script encountered an error");
                    LOGGER.error(e);
                }
            }
        };
        scriptThread.start();

    }

    public void executeInterpreter() throws ScriptFailureException {
        Interpreter pythonInterpreter = Interpreter.createInstance();
        executeScript(pythonInterpreter);
    }

    protected abstract void executeScript(Interpreter pythonInterpreter) throws ScriptFailureException;

    public abstract String extractSignature() throws ParseException, IOException;
}