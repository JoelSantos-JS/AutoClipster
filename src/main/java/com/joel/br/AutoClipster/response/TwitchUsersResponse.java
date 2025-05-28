package com.joel.br.AutoClipster.response;

import com.joel.br.AutoClipster.model.TwitchUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwitchUsersResponse {

    private List<TwitchUser> data;
}
