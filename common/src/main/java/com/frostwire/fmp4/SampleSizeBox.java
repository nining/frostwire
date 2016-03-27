/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2016, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frostwire.fmp4;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author gubatron
 * @author aldenml
 */
public final class SampleSizeBox extends EntryBaseBox {

    protected int sample_size;
    protected int sample_count;
    protected Entry[] entries;

    SampleSizeBox() {
        super(stsz);
    }

    @Override
    void read(InputChannel ch, ByteBuffer buf) throws IOException {
        super.read(ch, buf);

        IO.read(ch, 8, buf);
        sample_size = buf.getInt();
        sample_count = buf.getInt();
        if (sample_size == 0) {
            entries = new Entry[sample_count];
            for (int i = 0; i < sample_count; i++) {
                Entry e = new Entry();
                IO.read(ch, 4, buf);
                e.entry_size = buf.getInt();
                entries[i] = e;
            }
        }
    }

    @Override
    void writeFields(OutputChannel ch, ByteBuffer buf) throws IOException {
        buf.putInt(sample_size);
        buf.putInt(sample_count);
        IO.write(ch, 8, buf);
    }

    @Override
    int entryCount() {
        return entries != null ? sample_count : 0;
    }

    @Override
    int entrySize() {
        return 4;
    }

    @Override
    void putEntry(int i, ByteBuffer buf) {
        Entry e = entries[i];
        buf.putInt(e.entry_size);
    }

    @Override
    void update() {
        long s = 0;
        s += 4; // full box
        s += 8;
        s += entries != null ? sample_count * 4 : 0;
        length(s);
    }

    public static final class Entry {
        public int entry_size;
    }
}
