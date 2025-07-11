/*  Copyright (C) 2025 Thomas Kuehne

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.btbr.actions;

import android.bluetooth.BluetoothSocket;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

import nodomain.freeyourgadget.gadgetbridge.service.btbr.BtBRAction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.SocketCallback;

/// Invokes the given function and expects no {@link SocketCallback} result.
/// The transaction is aborted if the function throws an {@link Exception} or, if applicable,
/// returns {@code false}.
public class FunctionAction extends BtBRAction {
    private static final Logger LOG = LoggerFactory.getLogger(FunctionAction.class);

    private final Runnable mRunnable;
    private final Predicate<? super BluetoothSocket> mPredicate;

    public FunctionAction(@NonNull Runnable runnable) {
        mPredicate = null;
        mRunnable = runnable;
    }

    public FunctionAction(@NonNull Predicate<? super BluetoothSocket> predicate) {
        mPredicate = predicate;
        mRunnable = null;
    }

    @Override
    public boolean run(BluetoothSocket socket) {
        try {
            final boolean success;
            if (mRunnable != null) {
                mRunnable.run();
                success = true;
            } else if (mPredicate != null) {
                success = mPredicate.test(socket);
                if (!success) {
                    LOG.info("aborting transaction because function returned false");
                }
            } else {
                LOG.warn("aborting transaction because function is (null)");
                success = false;
            }
            return success;
        } catch (Exception e) {
            LOG.warn("aborting transaction because function threw exception", e);
            return false;
        }
    }

    @Override
    public boolean expectsResult() {
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        final String function;
        if (mRunnable != null) {
            function = mRunnable.getClass().getSimpleName();
        } else if (mPredicate != null) {
            function = mPredicate.getClass().getSimpleName();
        } else {
            function = "(null)";
        }
        return getCreationTime() + ": " + getClass().getSimpleName() + " " + function;
    }
}
