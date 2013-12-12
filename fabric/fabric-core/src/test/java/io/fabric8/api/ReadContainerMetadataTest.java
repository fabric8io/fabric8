/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import io.fabric8.utils.Base64Encoder;
import org.junit.Test;

public class ReadContainerMetadataTest {

    // TODO note this test will break as soon as we change the Container options as its using some
    // serialised data taken from ZK
    // so we can just disable it then; or regenerate this data or something...
    //
    // this test was created to diagnose character encoding woes on IDE :)

	//@Test
	public void testRead() throws Exception {
		String encoded = "rO0ABXNyADZvcmcuZnVzZXNvdXJjZS5mYWJyaWMuYXBpLkNyZWF0ZUNvbnRhaW5lckNoaWxkTWV0YWRhdGGrcHsKRAApDQIAAHhyADZvcmcuZnVzZXNvdXJjZS5mYWJyaWMuYXBpLkNyZWF0ZUNvbnRhaW5lckJhc2ljTWV0YWRhdGHTIy3GmkttfQIAA0wAFWNvbnRhaW5lckNvbmZndXJhdGlvbnQAD0xqYXZhL3V0aWwvTWFwO0wADWNvbnRhaW5lck5hbWV0ABJMamF2YS9sYW5nL1N0cmluZztMAA1jcmVhdGVPcHRpb25zdAAyTG9yZy9mdXNlc291cmNlL2ZhYnJpYy9hcGkvQ3JlYXRlQ29udGFpbmVyT3B0aW9uczt4cHNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAAAeHQABWV4bXExc3IANW9yZy5mdXNlc291cmNlLmZhYnJpYy5hcGkuQ3JlYXRlQ29udGFpbmVyQ2hpbGRPcHRpb25zQT8xyvH0zLwCAAB4cgA1b3JnLmZ1c2Vzb3VyY2UuZmFicmljLmFwaS5DcmVhdGVDb250YWluZXJCYXNpY09wdGlvbnMgKFoWgltepgIADloAC2FkbWluQWNjZXNzWgAOZW5zZW1ibGVTZXJ2ZXJMAAdqdm1PcHRzcQB+AANMAAttZXRhZGF0YU1hcHEAfgACTAAEbmFtZXEAfgADTAAGbnVtYmVydAATTGphdmEvbGFuZy9JbnRlZ2VyO0wABnBhcmVudHEAfgADTAAQcHJlZmVycmVkQWRkcmVzc3EAfgADTAAMcHJvdmlkZXJUeXBlcQB+AANMAAtwcm92aWRlclVSSXQADkxqYXZhL25ldC9VUkk7TAAIcHJveHlVcmlxAH4ADEwACHJlc29sdmVycQB+AANMABBzeXN0ZW1Qcm9wZXJ0aWVzcQB+AAJMAAx6b29rZWVwZXJVcmxxAH4AA3hwAABwc3EAfgAGP0AAAAAAAAx3CAAAABAAAAAAeHQABWV4bXExc3IAEWphdmEubGFuZy5JbnRlZ2VyEuKgpPeBhzgCAAFJAAV2YWx1ZXhyABBqYXZhLmxhbmcuTnVtYmVyhqyVHQuU4IsCAAB4cAAAAAF0AARyb290cHQABWNoaWxkc3IADGphdmEubmV0LlVSSawBeC5DnkmrAwABTAAGc3RyaW5ncQB+AAN4cHQADGNoaWxkOi8vcm9vdHhzcQB+ABV0ACpodHRwOi8vc3RyYWNtYWMubG9jYWw6ODE4MS9tYXZlbi9kb3dubG9hZC94dAANbG9jYWxob3N0bmFtZXNxAH4ABj9AAAAAAAAMdwgAAAAQAAAAAHh0ABNzdHJhY21hYy5sb2NhbDoyMTgx";

		byte[] decoded = Base64Encoder.decode(encoded).getBytes(Base64Encoder.base64CharSet);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded));
		CreateContainerMetadata metadata = (CreateContainerMetadata) ois.readObject();
		System.out.println("Found: " + metadata);
	}

    @Test
    public void dummy() {
    }
}

