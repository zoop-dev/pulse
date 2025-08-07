FROM ghcr.io/alvr/alpine-android:android-33-jdk21

ARG USERNAME=developer
ARG USER_UID=1000
ARG USER_GID=$USER_UID

RUN apk --no-cache add git \
    sudo \ 
    nano \
    bash \
    bash-completion && \
    # setup sudo user and permissions
    addgroup -g $USER_GID $USERNAME && \
    adduser -u $USER_UID -G $USERNAME $USERNAME --disabled-password -s /bin/bash && \
    echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME && \
    chmod 0440 /etc/sudoers.d/$USERNAME && \
    echo $USERNAME:10000:5000 > /etc/subuid; \
    echo $USERNAME:10000:5000 > /etc/subgid; \
    chown -R developer:developer /opt/sdk/

USER $USERNAME

# configure environment
RUN bash -c "$(curl -fsSL https://raw.githubusercontent.com/ohmybash/oh-my-bash/master/tools/install.sh)" && \
    # setup editor
    echo "export EDITOR=/usr/bin/nano" >> ~/.bashrc