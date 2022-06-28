package de.intension.protocol.oidc.mappers;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class HmacPairwiseSubMapperTest
{

    @Test
    void should_map_userId_to_sub()
    {
        var mapper = new HmacPairwiseSubMapper();

        assertThat(mapper.getIdPrefix(), equalTo("hmac"));
    }
}
