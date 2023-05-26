// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

suite("test_dup_tab_decimal") {

    def table1 = "test_dup_tab_decimal"
    sql  "ADMIN SET FRONTEND CONFIG ('disable_decimalv2' = 'false')"

    sql "drop table if exists ${table1}"

    sql """
CREATE TABLE IF NOT EXISTS `${table1}` (
  `siteid` decimal(10, 5) NOT NULL COMMENT "",
  `citycode` decimal(10, 5) NOT NULL COMMENT "",
  `userid` decimal(10, 5) NOT NULL COMMENT "",
  `pv` decimal(10, 5) NOT NULL COMMENT ""
) ENGINE=OLAP
DUPLICATE KEY(`siteid`)
COMMENT "OLAP"
DISTRIBUTED BY HASH(`siteid`) BUCKETS 1
PROPERTIES (
"replication_allocation" = "tag.location.default: 1",
"in_memory" = "false",
"storage_format" = "V2"
)
    """

    sql """insert into ${table1} values(1.1,2.2,2.3,3.4),
        (1.1,1.2,1.3,1.4),
        (2.1,2.2,2.3,2.4),
        (3.1,3.2,3.3,3.4),
        (4.1,4.2,4.3,4.4)
"""

    // query decimal
    test {
        sql "select siteid from ${table1} order by siteid"
        result([[1.100000000],[1.100000000],[2.100000000],[3.100000000],[4.100000000]])
    }
    test {
        sql "select siteid,citycode from ${table1} order by siteid,citycode"
        result([[1.100000000,1.200000000],[1.100000000,2.200000000],[2.100000000,2.200000000],[3.100000000,3.200000000],[4.100000000,4.200000000]])
    }

    // pred is decimal
    test {
        sql "select siteid from ${table1} where siteid=4.1"
        result([[4.100000000]])
    }
    test {
        sql "select siteid from ${table1} where siteid=1.1"
        result([[1.100000000],[1.100000000]])
    }

    // pred not key
    test {
        sql "select citycode from ${table1} where citycode=2.2"
        result([[2.200000000],[2.200000000]])
    }
    test {
        sql "select citycode from ${table1} where citycode=4.2"
        result([[4.200000000]])
    }

    // pred column not same with read column
    test {
        sql "select citycode from ${table1} where siteid=1.1 order by citycode"
        result([[1.200000000],[2.200000000]])
    }

    test {
        sql "select citycode from ${table1} where siteid=4.1 order by citycode"
        result([[4.200000000]])
    }

    // pred column not same with read column;pred is not key
    test {
        sql "select siteid from ${table1} where citycode=2.2 order by siteid"
        result([[1.100000000],[2.100000000]])
    }

    test {
        sql "select siteid from ${table1} where citycode=4.2 order by siteid"
        result([[4.100000000]])
    }

    // int pred
    test {
        sql "select siteid from ${table1} where siteid in(4.1)"
        result([[4.100000000]])
    }

    test {
        sql "select * from ${table1} where siteid in(4.1)"
        result([[4.100000000,4.200000000,4.300000000,4.400000000]])
    }

    test {
        sql "select userid from ${table1} where userid in(2.3)"
        result([[2.300000000],[2.300000000]])
    }

    test {
        sql "select userid from ${table1} where userid not in(2.3) order by userid"
        result([[1.300000000],[3.300000000],[4.300000000]])
    }

    sql "drop table if exists ${table1}"

}
